package com.qiaoyuang.movie.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.local.MovieLocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Reads a cached list out of the database. Paired with [MovieRemoteMediator], which is the only
 * thing that talks to the network: the UI renders whatever is cached, then updates when the
 * mediator has written more.
 *
 * Keys are offsets rather than page numbers, because the cache is queried with LIMIT/OFFSET and
 * has no notion of which server page a row came from. Every offset this source produces is a
 * multiple of [pageSize] — pages are read with a fixed limit, so a key off that grid would make
 * neighbouring pages overlap and Paging would present the same movie twice.
 */
internal class MovieListPagingSource(
    private val local: MovieLocalDataSource,
    private val listKey: String,
    scope: CoroutineScope,
    private val pageSize: Int = MOVIE_PAGE_SIZE,
) : PagingSource<Int, Movie>() {

    init {
        // sqllin cannot report its own table changes, so the tracker does. Reading the version
        // before subscribing closes the gap between construction and collection: a write that
        // lands in it is already past `since`, so it invalidates immediately instead of being
        // missed and leaving this source serving stale rows for good.
        val since = local.invalidationTracker.versionOf(listKey)
        val job = scope.launch {
            local.invalidationTracker.invalidationOf(listKey, since).collect { invalidate() }
        }
        registerInvalidatedCallback { job.cancel() }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val offset = params.key ?: FIRST_OFFSET
        val movies = local.moviesIn(listKey, limit = params.loadSize, offset = offset)
        return LoadResult.Page(
            data = movies,
            // Backwards paging matters here even though the remote list only grows downwards:
            // the mediator invalidates this source on every write, and the refresh that follows
            // resumes at the user's position rather than at the top. Without a prevKey the rows
            // above that position would be unreachable — the list would simply lose its start.
            prevKey = if (offset == FIRST_OFFSET) null else maxOf(FIRST_OFFSET, offset - pageSize),
            // A short page means the cache is exhausted, not that the list has ended. Reporting
            // no next key is exactly what makes Paging ask the mediator to append, which is how
            // the next server page gets fetched.
            nextKey = if (movies.size < params.loadSize) null else offset + movies.size,
        )
    }

    /**
     * Where to resume after an invalidation. anchorPosition is an index into the *presented*
     * list, which starts at the first loaded page's offset — recoverable from that page's
     * prevKey, since prevKey is exactly one page below it (and null at the top).
     *
     * Returning null instead would restart at offset zero on every mediator write and throw
     * away the pages the user had scrolled through.
     */
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        val anchor = state.anchorPosition ?: return null
        val firstLoadedOffset = state.pages.firstOrNull()
            ?.let { page -> page.prevKey?.plus(pageSize) ?: FIRST_OFFSET }
            ?: FIRST_OFFSET
        // Snapped down to the page grid; see the class comment.
        return (firstLoadedOffset + anchor) / pageSize * pageSize
    }

    private companion object {
        const val FIRST_OFFSET = 0
    }
}
