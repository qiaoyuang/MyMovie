package com.qiaoyuang.movie.model.local

import androidx.paging.PagingSource
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
 * to the implementation.
 *
 * An interface so the RemoteMediator and the repository can be tested against an in-memory fake.
 * The implementation itself is tested against a real database, on Robolectric.
 *
 * Implementations are main-safe.
 */
internal interface MovieLocalDataSource {

    /**
     * A cached list, paged straight out of the database. It invalidates itself whenever a write
     * through the same database changes a table it reads — which is how the mediator's writes
     * reach the screen without anything announcing them.
     */
    fun pagingSource(listKey: String): PagingSource<Int, MovieEntity>

    /** One page of a cached list, ordered as the server returned it — for the detail strip. */
    suspend fun moviesIn(listKey: String, limit: Int, offset: Int): List<Movie>

    suspend fun cursorOf(listKey: String): ListCursor?

    /** REFRESH: discards the list and stores [movies] as its first page. */
    suspend fun replaceList(listKey: String, pageSize: Int, movies: List<Movie>, cursor: ListCursor)

    /** APPEND: adds page [page], dropping movies the list already holds. */
    suspend fun appendPage(listKey: String, page: Int, pageSize: Int, movies: List<Movie>, cursor: ListCursor)

    suspend fun movie(id: Long): Movie?

    /** Stores movies without touching any list's membership — used by the detail screen. */
    suspend fun upsertMovies(movies: List<Movie>)

    suspend fun genres(): List<MovieGenre>

    /** When the catalogue was last fetched, or null if it never has been. */
    suspend fun genresRefreshedAt(): Long?

    /** Replaces the catalogue and its refresh stamp together, so neither can outlive the other. */
    suspend fun replaceGenres(genres: List<MovieGenre>, refreshedAt: Long)
}
