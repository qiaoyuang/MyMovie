package com.qiaoyuang.movie.model.local

import com.ctrip.sqllin.dsl.Database
import com.ctrip.sqllin.dsl.DatabaseScope
import com.ctrip.sqllin.dsl.sql.X
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The only holder of the sqllin [Database]. Keeping the connection private is what makes
 * [invalidationTracker] trustworthy: no write can happen without going through a method here.
 *
 * Columns are always referenced as `table.x`, never bare: a bare name would resolve to the
 * enclosing function's parameter of the same name (listKey, page) rather than to the column.
 */
internal class MovieLocalDataSourceImpl(
    private val database: Database,
    private val ioDispatcher: CoroutineDispatcher,
) : MovieLocalDataSource {

    override val invalidationTracker: TableInvalidationTracker = TableInvalidationTracker()

    /**
     * sqllin collects a scope's statements and executes them when the scope exits, so a SELECT's
     * results are readable only after the block returns — a read-then-write cannot be expressed
     * as one transaction. This mutex is what makes appendPage's "which movies do we already
     * have" read atomic with the insert that acts on it. It covers writers only; a read that
     * races a write costs at worst one redundant fetch.
     */
    private val writeMutex = Mutex()

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

    /**
     * Counted in Kotlin rather than with COUNT(*), because a sqllin SELECT deserialises into the
     * entity type and has no column projection. A list holds at most a few hundred three-column
     * rows, and this runs once per RemoteMediator decision, not per scroll.
     */
    override suspend fun countIn(listKey: String): Int = withContext(ioDispatcher) {
        var entries: SelectStatement<MovieListEntryEntity>? = null
        database suspendedScope {
            MovieListEntryEntityTable { table -> entries = table SELECT WHERE(table.listKey EQ listKey) }
        }
        entries?.getResults()?.size ?: 0
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
        writeMutex.withLock {
            database suspendedScope {
                transaction {
                    MovieListEntryEntityTable { table -> table DELETE WHERE(table.listKey EQ listKey) }
                    writeMovies(fresh)
                    writeEntries(listEntriesFor(listKey, FIRST_PAGE, pageSize, fresh.map(Movie::id)))
                    writeCursor(listKey, cursor)
                }
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
        writeMutex.withLock {
            var stored: SelectStatement<MovieListEntryEntity>? = null
            database suspendedScope {
                MovieListEntryEntityTable { table -> stored = table SELECT WHERE(table.listKey EQ listKey) }
            }
            val storedIds = stored?.getResults()
                ?.mapTo(mutableSetOf(), MovieListEntryEntity::movieId)
                .orEmpty()
            val fresh = movies.filterAlreadyStored(storedIds)
            database suspendedScope {
                transaction {
                    writeMovies(fresh)
                    writeEntries(listEntriesFor(listKey, page, pageSize, fresh.map(Movie::id)))
                    writeCursor(listKey, cursor)
                }
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
        writeMutex.withLock {
            database suspendedScope { transaction { writeMovies(movies.distinctMovies()) } }
        }
    }

    override suspend fun genres(): List<MovieGenre> = withContext(ioDispatcher) {
        var statement: SelectStatement<GenreEntity>? = null
        database suspendedScope { GenreEntityTable { table -> statement = table SELECT X } }
        statement?.getResults()?.map(GenreEntity::toDomain).orEmpty()
    }

    /** The catalogue is small and always fetched whole, so replacing it also drops retired ids. */
    override suspend fun replaceGenres(genres: List<MovieGenre>) = withContext(ioDispatcher) {
        writeMutex.withLock {
            database suspendedScope {
                transaction {
                    GenreEntityTable { table ->
                        table DELETE X
                        if (genres.isNotEmpty()) table INSERT_OR_REPLACE genres.map(MovieGenre::toEntity)
                    }
                }
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

    /** Plain INSERT: the composite primary key is what makes a surviving duplicate fail loudly. */
    private fun DatabaseScope.writeEntries(entries: List<MovieListEntryEntity>) {
        if (entries.isEmpty()) return
        MovieListEntryEntityTable { table -> table INSERT entries }
    }

    private fun DatabaseScope.writeCursor(listKey: String, cursor: ListCursor) {
        ListRemoteKeyEntityTable { table -> table INSERT_OR_REPLACE cursor.toEntity(listKey) }
    }
}
