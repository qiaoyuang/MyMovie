package com.qiaoyuang.movie.model

import androidx.collection.MutableLongSet
import androidx.paging.PagingConfig
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

    /**
     * Ids already handed to Paging. TMDB pages a result set that keeps moving, so the same movie
     * can come back on two pages when the ordering shifts between requests — and two items with
     * the same id make LazyColumn's key collide, which throws rather than rendering twice.
     *
     * The DB-backed lists get this for free: their (listKey, movieId) primary key rejects the
     * duplicate on insert. This is the same guarantee for the one list that never reaches the
     * database.
     *
     * Instance state is the right scope. A PagingSource lives for exactly one generation, and
     * Paging builds a new one on refresh, so the set is emptied precisely when the pages it
     * describes are thrown away — which is also what makes a refresh show those movies again
     * rather than filtering them all out. No synchronisation is needed: this source never sets a
     * prevKey, so Paging only ever runs the initial load and then appends, one at a time.
     *
     * A primitive set rather than mutableSetOf<Long>(): every TMDB id is far above Long's boxing
     * cache, so the boxed form costs ~70 bytes per id against ~15, and allocates twenty throwaway
     * boxes per page. Nothing here needs insertion order.
     *
     * **This assumes [PagingConfig.maxSize] stays unbounded, which is its default.** With a bound,
     * Paging drops pages at the ends and reloads them when the user scrolls back — and every id in
     * a reloaded page is already in this set, so the page would come back empty and its movies
     * would silently vanish. Setting maxSize means tracking the ids per page instead, so that
     * reloading one de-duplicates against the others only.
     */
    private val emittedIds = MutableLongSet()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        // params.loadSize is deliberately ignored: the endpoint is paginated by page number,
        // not by offset/limit, so Paging's requested size cannot be honoured.
        val page = params.key ?: FIRST_PAGE
        return when (val result = fetchPage(page)) {
            is Result.Success<MovieResponse> -> LoadResult.Page(
                // add() reports whether the id is new, so this drops a repeat of an earlier
                // page and a repeat within this one in a single pass. A page left shorter —
                // even empty — does not end pagination here, because nextKey is what decides
                // that for a page-numbered endpoint.
                data = result.data.results.filter { emittedIds.add(it.id) },
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
