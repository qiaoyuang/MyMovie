package com.qiaoyuang.movie.model.local

import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre

/**
 * How far a cached list has been paged, and when it was last refreshed.
 *
 * Deliberately not [ListRemoteKeyEntity]: callers always pass the listKey as an argument, so
 * carrying it inside the object too would create two places for it to disagree.
 */
internal data class ListCursor(
    /** Next page to request, or null once the server has no more. */
    val nextPage: Int?,
    val totalPages: Int,
    /** Epoch millis; the TTL compares against this. */
    val lastRefreshedAt: Long,
)

/**
 * The offline cache, as the rest of the data layer sees it. The sqllin Database is visible only
 * to the implementation, so every write passes through here and can announce itself to
 * [invalidationTracker].
 *
 * An interface, for two reasons: the RemoteMediator and PagingSource built on top of it in later
 * phases can be tested against a fake, and sqllin's Android driver talks to
 * android.database.sqlite, which host tests cannot load — so the real implementation is the one
 * part of this layer with no automated coverage, and it is kept as thin as possible.
 *
 * Implementations are main-safe.
 */
internal interface MovieLocalDataSource {

    val invalidationTracker: TableInvalidationTracker

    /** One page of a cached list, ordered as the server returned it. */
    suspend fun moviesIn(listKey: String, limit: Int, offset: Int): List<Movie>

    /** How many movies the list holds; 0 means nothing is cached for it. */
    suspend fun countIn(listKey: String): Int

    suspend fun cursorOf(listKey: String): ListCursor?

    /** REFRESH: discards the list and stores [movies] as its first page. */
    suspend fun replaceList(listKey: String, pageSize: Int, movies: List<Movie>, cursor: ListCursor)

    /** APPEND: adds page [page], dropping movies the list already holds. */
    suspend fun appendPage(listKey: String, page: Int, pageSize: Int, movies: List<Movie>, cursor: ListCursor)

    suspend fun movie(id: Long): Movie?

    /** Stores movies without touching any list's membership — used by the detail screen. */
    suspend fun upsertMovies(movies: List<Movie>)

    suspend fun genres(): List<MovieGenre>

    suspend fun replaceGenres(genres: List<MovieGenre>)
}
