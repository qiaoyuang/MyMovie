package com.qiaoyuang.movie.model.local

import androidx.paging.PagingSource
import com.ctrip.sqllin.dsl.Database
import com.ctrip.sqllin.dsl.DatabaseScope
import com.ctrip.sqllin.dsl.annotation.ExperimentalDSLDatabaseAPI
import com.ctrip.sqllin.dsl.paging.pagingSource
import com.ctrip.sqllin.dsl.sql.X
import com.ctrip.sqllin.dsl.sql.clause.EQ
import com.ctrip.sqllin.dsl.sql.clause.IN
import com.ctrip.sqllin.dsl.sql.clause.INNER_JOIN
import com.ctrip.sqllin.dsl.sql.clause.LIMIT
import com.ctrip.sqllin.dsl.sql.clause.OFFSET
import com.ctrip.sqllin.dsl.sql.clause.ON
import com.ctrip.sqllin.dsl.sql.clause.ORDER_BY
import com.ctrip.sqllin.dsl.sql.clause.OrderByWay.ASC
import com.ctrip.sqllin.dsl.sql.clause.WHERE
import com.ctrip.sqllin.dsl.sql.statement.SelectStatement
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The only holder of the sqllin [Database].
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

    @OptIn(ExperimentalDSLDatabaseAPI::class)
    override fun pagingSource(listKey: String): PagingSource<Int, MovieEntity> =
        database.pagingSource(ioDispatcher) { listPage(listKey) }

    override suspend fun moviesIn(listKey: String, limit: Int, offset: Int): List<Movie> =
        withContext(ioDispatcher) {
            var page: SelectStatement<MovieEntity>? = null
            database suspendedScope { page = listPage(listKey) LIMIT limit OFFSET offset }
            page?.getResults().orEmpty().map(MovieEntity::toDomain)
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
    }

    override suspend fun movie(id: Long): Movie? =
        withContext(ioDispatcher) { moviesByIds(listOf(id)).firstOrNull() }

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
     * A list's movies in order, as one SELECT: its entries joined to their rows. One statement is
     * what lets the paging source read a page, and what makes the source watch both tables — so
     * a refreshed movie title reaches a list on screen as well as a new page does.
     */
    @OptIn(ExperimentalDSLDatabaseAPI::class)
    private fun DatabaseScope.listPage(listKey: String) =
        (MovieListEntryEntityTable INNER_JOIN MovieEntityTable ON
            (MovieListEntryEntityTable.movieId EQ MovieEntityTable.id)) SELECT
            WHERE<MovieEntity>(MovieListEntryEntityTable.listKey EQ listKey) ORDER_BY
            (MovieListEntryEntityTable.position to ASC)

    private suspend fun moviesByIds(ids: List<Long>): List<Movie> {
        var movies: SelectStatement<MovieEntity>? = null
        database suspendedScope {
            MovieEntityTable { table -> movies = table SELECT WHERE(table.id IN ids) }
        }
        val byId = movies?.getResults().orEmpty().associate { it.id to it.toDomain() }
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
        // Replacing the row replaces its genre ids with it, so a genre set that shrank between
        // fetches needs no separate cleanup.
        MovieEntityTable { table -> table INSERT_OR_REPLACE movies.map(Movie::toEntity) }
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
