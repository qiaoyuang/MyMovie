package com.qiaoyuang.movie.model

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.local.ListCursor
import com.qiaoyuang.movie.model.local.MovieLocalDataSource
import com.qiaoyuang.movie.model.local.currentTimeMillis
import com.qiaoyuang.movie.model.local.isStale
import kotlinx.coroutines.CancellationException

/**
 * The network half of an offline-first paged list: it only ever writes to the cache, and the
 * DB-backed [MovieListPagingSource] is what the UI actually reads. That split is what lets a
 * warm cache render before any request is made, and at all when there is no network.
 *
 * Generic over the endpoint the same way [MoviePagingSource] is — top-rated and similar-movies
 * differ only in [listKey] and [fetchPage].
 */
@OptIn(ExperimentalPagingApi::class)
internal class MovieRemoteMediator(
    private val listKey: String,
    private val local: MovieLocalDataSource,
    private val fetchPage: suspend (page: Int) -> Result<MovieResponse, MovieDataException>,
    private val now: () -> Long = ::currentTimeMillis,
) : RemoteMediator<Int, Movie>() {

    /**
     * Whether opening the screen should hit the network at all. SKIP leaves the PagingSource as
     * the only loader, so a cached list appears immediately and without a request; the TTL is
     * the sole thing that forces a refresh, since there is no eviction policy.
     */
    override suspend fun initialize(): InitializeAction {
        val cursor = local.cursorOf(listKey)
        return if (cursor == null || isStale(cursor.lastRefreshedAt, now()))
            InitializeAction.LAUNCH_INITIAL_REFRESH
        else
            InitializeAction.SKIP_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, Movie>): MediatorResult {
        // The list only grows downwards; nothing ever sets a prevKey.
        if (loadType == LoadType.PREPEND) return MediatorResult.Success(endOfPaginationReached = true)

        val previous = local.cursorOf(listKey)
        val page = when (loadType) {
            LoadType.REFRESH -> FIRST_PAGE
            // The cursor, not the PagingState, is the source of truth for how far the remote
            // list has been read — the state describes cached rows, which say nothing about
            // which server page they came from.
            else -> previous?.nextPage ?: return MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            when (val result = fetchPage(page)) {
                is Result.Error<MovieDataException> -> MediatorResult.Error(result.error)
                is Result.Success<MovieResponse> -> {
                    val response = result.data
                    val cursor = ListCursor(
                        nextPage = if (page < response.totalPages) page + 1 else null,
                        totalPages = response.totalPages,
                        // Only a refresh restarts the TTL. If appending did too, paging deep
                        // into a list would keep renewing it and page one would never be
                        // re-fetched.
                        lastRefreshedAt = when (loadType) {
                            LoadType.REFRESH -> now()
                            else -> previous?.lastRefreshedAt ?: now()
                        },
                    )
                    if (loadType == LoadType.REFRESH)
                        local.replaceList(listKey, MOVIE_PAGE_SIZE, response.results, cursor)
                    else
                        local.appendPage(listKey, page, MOVIE_PAGE_SIZE, response.results, cursor)
                    MediatorResult.Success(endOfPaginationReached = cursor.nextPage == null)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // fetchPage already reports its own failures as Result.Error, so what reaches here
            // is a cache write that failed.
            MediatorResult.Error(e)
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
