package com.qiaoyuang.movie.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse

/** TMDB returns a fixed 20 results per page and ignores any page-size parameter. */
internal const val MOVIE_PAGE_SIZE = 20

/**
 * Drives any of TMDB's page-numbered movie list endpoints. Top-rated, similar-movies and
 * search all answer with the same [MovieResponse] shape and differ only in which repository
 * call they make, so the endpoint arrives as [fetchPage] instead of being subclassed.
 *
 * The page cursor lives in [LoadResult.Page.nextKey] rather than in a field on a ViewModel,
 * which is what makes it impossible for the cursor and the loaded data to drift apart.
 */
internal class MoviePagingSource(
    private val fetchPage: suspend (page: Int) -> Result<MovieResponse, MovieDataException>,
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        // params.loadSize is deliberately ignored: the endpoint is paginated by page number,
        // not by offset/limit, so Paging's requested size cannot be honoured.
        val page = params.key ?: FIRST_PAGE
        return when (val result = fetchPage(page)) {
            is Result.Success<MovieResponse> -> LoadResult.Page(
                data = result.data.results,
                // No backwards paging: the list only ever grows downwards.
                prevKey = null,
                // Derived from the requested page rather than the one the response echoes
                // back, so the keys are strictly increasing by construction. Paging rejects
                // a repeated nextKey outright, so a server that misreports its page number
                // would otherwise break pagination.
                nextKey = if (page < result.data.totalPages) page + 1 else null,
            )
            // MovieDataException is already a Throwable, so it goes to Paging unwrapped and the
            // UI can still tell what kind of failure it was.
            is Result.Error<MovieDataException> -> LoadResult.Error(result.error)
        }
    }

    /**
     * Returning null restarts from [FIRST_PAGE] on refresh. Anchoring the refresh around the
     * user's current position would need prevKey support, which this endpoint does not have.
     */
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null

    private companion object {
        const val FIRST_PAGE = 1
    }
}
