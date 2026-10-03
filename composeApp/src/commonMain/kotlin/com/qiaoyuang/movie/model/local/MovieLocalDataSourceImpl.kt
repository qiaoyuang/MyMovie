package com.qiaoyuang.movie.model.local

import com.ctrip.sqllin.dsl.Database
import com.ctrip.sqllin.dsl.DatabaseScope
import com.ctrip.sqllin.dsl.sql.X
import com.ctrip.sqllin.dsl.sql.clause.AS
import com.ctrip.sqllin.dsl.sql.clause.count
import com.ctrip.sqllin.dsl.sql.clause.EQ
import com.ctrip.sqllin.dsl.sql.clause.IN
import com.ctrip.sqllin.dsl.sql.clause.LIMIT
import com.ctrip.sqllin.dsl.sql.clause.OFFSET
import com.ctrip.sqllin.dsl.sql.clause.ORDER_BY
import com.ctrip.sqllin.dsl.sql.clause.OrderByWay.ASC
import com.ctrip.sqllin.dsl.sql.clause.WHERE
import com.ctrip.sqllin.dsl.sql.statement.SelectStatement
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The only holder of the sqllin [Database]. Keeping the connection private is what makes
 * [invalidationTracker] trustworthy: no write can happen without going through a method here.
 *
 * Every method is a single sqllin scope, and each scope's writes are one transaction, so none of
 * them needs a lock of its own: sqllin serialises the execution of scopes, and conflicts between
 * rows are resolved by the statements themselves rather than by reading first.
 *
 * Columns are always referenced as `table.x`, never bare: a bare name would resolve to the
 * enclosing function's parameter of the same name (listKey, page) rather than to the column.
 */
internal class MovieLocalDataSourceImpl(
    private val database: Database,
    private val ioDispatcher: CoroutineDispatcher,
) : MovieLocalDataSource {

    override val invalidationTracker: TableInvalidationTracker = TableInvalidationTracker()

    override suspend fun moviesIn(listKey: String, limit: Int, offset: Int): List<Movie> =
        withContext(ioDispatcher) {
            var entries: SelectStatement<MovieListEntryEntity>? = null
            database suspendedScope {
                MovieListEntryEntityTable { table ->
                    entries = table SELECT WHERE(table.listKey EQ listKey) ORDER_BY
                        (table.position to ASC) LIMIT limit OFFSET offset
                }
            }
            val ordered = entries?.getResults().orEmpty()
            if (ordered.isEmpty()) emptyList() else moviesByIds(ordered.map(MovieListEntryEntity::movieId))
        }

    /** A real COUNT(*), read into [ListEntryCount] — see its doc for why the type exists. */
    override suspend fun countIn(listKey: String): Int = withContext(ioDispatcher) {
        var count: SelectStatement<ListEntryCount>? = null
        database suspendedScope {
            MovieListEntryEntityTable { table ->
                count = table SELECT (count(X) AS ListEntryCount::entries) WHERE (table.listKey EQ listKey)
            }
        }
        // An aggregate without GROUP BY always returns exactly one row, zero included.
        count?.getResults()?.single()?.entries?.toInt() ?: 0
    }

    override suspend fun cursorOf(listKey: String): ListCursor? = withContext(ioDispatcher) {
        var keys: SelectStatement<ListRemoteKeyEntity>? = null
        database suspendedScope {
            ListRemoteKeyEntityTable { table -> keys = table SELECT WHERE(table.listKey EQ listKey) }
        }
        keys?.getResults()?.firstOrNull()?.toCursor()
    }

    override suspend fun replaceList(
        listKey: String,
        pageSize: Int,
        movies: List<Movie>,
        cursor: ListCursor,
    ) = withContext(ioDispatcher) {
        val fresh = movies.distinctMovies()
        database suspendedScope {
            transaction {
                MovieListEntryEntityTable { table -> table DELETE WHERE(table.listKey EQ listKey) }
                writeMovies(fresh)
                writeEntries(listEntriesFor(listKey, FIRST_PAGE, pageSize, fresh.map(Movie::id)))
                writeCursor(listKey, cursor)
            }
        }
        invalidationTracker.notifyChanged(listKey)
    }

    override suspend fun appendPage(
        listKey: String,
        page: Int,
        pageSize: Int,
        movies: List<Movie>,
        cursor: ListCursor,
    ) = withContext(ioDispatcher) {
        val fresh = movies.distinctMovies()
        database suspendedScope {
            transaction {
                writeMovies(fresh)
                writeEntries(listEntriesFor(listKey, page, pageSize, fresh.map(Movie::id)))
                writeCursor(listKey, cursor)
            }
        }
        invalidationTracker.notifyChanged(listKey)
    }

    override suspend fun movie(id: Long): Movie? =
        withContext(ioDispatcher) { moviesByIds(listOf(id)).firstOrNull() }

    /**
     * Does not notify [invalidationTracker]: it cannot know which lists hold these movies, and
     * the caller (the detail screen) reads a movie directly rather than through a list. A cached
     * list therefore keeps showing the older title until that list is refreshed.
     */
    override suspend fun upsertMovies(movies: List<Movie>) = withContext(ioDispatcher) {
        database suspendedScope { transaction { writeMovies(movies.distinctMovies()) } }
    }

    override suspend fun genres(): List<MovieGenre> = withContext(ioDispatcher) {
        var statement: SelectStatement<GenreEntity>? = null
        database suspendedScope { GenreEntityTable { table -> statement = table SELECT X } }
        statement?.getResults()?.map(GenreEntity::toDomain).orEmpty()
    }

    override suspend fun genresRefreshedAt(): Long? = cursorOf(GENRES_LIST_KEY)?.lastRefreshedAt

    /**
     * The catalogue is small and always fetched whole, so replacing it also drops retired ids.
     * The stamp goes in the same transaction: a fresh stamp with no genres behind it would stop
     * anything from ever fetching them again.
     */
    override suspend fun replaceGenres(genres: List<MovieGenre>, refreshedAt: Long) =
        withContext(ioDispatcher) {
            database suspendedScope {
                transaction {
                    GenreEntityTable { table ->
                        table DELETE X
                        if (genres.isNotEmpty())
                            table INSERT_OR_REPLACE genres.map(MovieGenre::toEntity)
                    }
                    writeCursor(
                        GENRES_LIST_KEY,
                        ListCursor(nextPage = null, totalPages = 1, lastRefreshedAt = refreshedAt),
                    )
                }
            }
        }

    /**
     * Two queries and an in-memory join instead of SQL JOINs: a sqllin JOIN needs a result class
     * matching the joined column list, which would mean a third schema type per query shape.
     * Both queries hit a primary-key index and return at most one page of rows.
     */
    private suspend fun moviesByIds(ids: List<Long>): List<Movie> {
        var movies: SelectStatement<MovieEntity>? = null
        var crossRefs: SelectStatement<MovieGenreEntity>? = null
        database suspendedScope {
            MovieEntityTable { table -> movies = table SELECT WHERE(table.id IN ids) }
            MovieGenreEntityTable { table -> crossRefs = table SELECT WHERE(table.movieId IN ids) }
        }
        val byId = movies?.getResults().orEmpty()
            .toDomain(crossRefs?.getResults().orEmpty())
            .associateBy(Movie::id)
        // SQLite promises no ordering for IN, so the caller's order is reapplied here.
        return ids.mapNotNull(byId::get)
    }

    /**
     * INSERT_OR_REPLACE, because the same movie legitimately arrives again — it can belong to
     * several lists, and a refresh re-fetches rows we already hold. Replacing is the semantics
     * wanted: the newer copy wins.
     */
    private fun DatabaseScope.writeMovies(movies: List<Movie>) {
        if (movies.isEmpty()) return
        MovieEntityTable { table -> table INSERT_OR_REPLACE movies.map(Movie::toEntity) }
        // A movie's genre set can shrink between fetches, so its rows are replaced, not merged.
        val ids = movies.map(Movie::id)
        MovieGenreEntityTable { table -> table DELETE WHERE(table.movieId IN ids) }
        val crossRefs = movies.flatMap(Movie::toGenreCrossRefs)
        if (crossRefs.isNotEmpty()) MovieGenreEntityTable { table -> table INSERT crossRefs }
    }

    /**
     * INSERT OR IGNORE is the de-duplication. TMDB can return the same movie on two pages when
     * the underlying ordering shifts between requests, which conflicts on (listKey, movieId);
     * the conflicting row is skipped and the movie keeps the position it already had, so an item
     * the user has scrolled past does not jump down the list. INSERT OR REPLACE would move it.
     *
     * This replaces a SELECT of the stored ids, a Kotlin filter and a mutex holding the two
     * together: one statement is atomic against other connections, not just other coroutines.
     */
    private fun DatabaseScope.writeEntries(entries: List<MovieListEntryEntity>) {
        if (entries.isEmpty()) return
        MovieListEntryEntityTable { table -> table INSERT_OR_IGNORE entries }
    }

    private fun DatabaseScope.writeCursor(listKey: String, cursor: ListCursor) {
        ListRemoteKeyEntityTable { table -> table INSERT_OR_REPLACE cursor.toEntity(listKey) }
    }
}
