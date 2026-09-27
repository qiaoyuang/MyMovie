package com.qiaoyuang.movie.test

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.qiaoyuang.movie.model.MOVIE_PAGE_SIZE
import com.qiaoyuang.movie.model.MovieDataException
import com.qiaoyuang.movie.model.MovieRemoteMediator
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.local.CACHE_TTL_MILLIS
import com.qiaoyuang.movie.model.local.ListCursor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediatorTest {

    private val listKey = "top_rated"
    private val nowMillis = 1_700_000_000_000L

    private fun movie(id: Long) = Movie(
        id = id,
        title = "title$id",
        overview = "overview$id",
        posterPath = null,
        backdropPath = null,
        voteAverage = 8.0,
        genreIds = null,
    )

    private fun page(page: Int, totalPages: Int, ids: LongRange) =
        MovieResponse(page = page, results = ids.map(::movie), totalPages = totalPages)

    private val emptyState = PagingState<Int, Movie>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = MOVIE_PAGE_SIZE),
        leadingPlaceholderCount = 0,
    )

    private fun mediator(
        local: FakeMovieLocalDataSource,
        now: Long = nowMillis,
        fetch: suspend (page: Int) -> Result<MovieResponse, MovieDataException>,
    ) = MovieRemoteMediator(listKey, local, fetch, now = { now })

    // ---- initialize: the TTL is the only thing that forces a network refresh ----

    @Test
    fun anEmptyCacheRefreshes() = runTest {
        val action = mediator(FakeMovieLocalDataSource()) { Result.Success(page(1, 5, 1L..20L)) }.initialize()
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, action)
    }

    @Test
    fun aFreshCacheSkipsTheNetworkEntirely() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(listKey, MOVIE_PAGE_SIZE, listOf(movie(1L)), ListCursor(2, 5, nowMillis))

        val action = mediator(local, now = nowMillis + CACHE_TTL_MILLIS - 1) {
            error("must not fetch")
        }.initialize()

        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, action)
    }

    @Test
    fun aCacheOlderThanTheTtlRefreshes() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(listKey, MOVIE_PAGE_SIZE, listOf(movie(1L)), ListCursor(2, 5, nowMillis))

        val action = mediator(local, now = nowMillis + CACHE_TTL_MILLIS) {
            Result.Success(page(1, 5, 1L..20L))
        }.initialize()

        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, action)
    }

    // ---- load ----

    @Test
    fun refreshReplacesTheCachedListWithPageOne() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(listKey, MOVIE_PAGE_SIZE, listOf(movie(99L)), ListCursor(2, 5, 0L))

        val result = mediator(local) { requestedPage ->
            assertEquals(1, requestedPage)
            Result.Success(page(1, 5, 1L..20L))
        }.load(LoadType.REFRESH, emptyState)

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        // The stale movie is gone rather than merged in.
        assertEquals((1L..20L).toList(), local.positionsIn(listKey).map { it.second })
        assertEquals(ListCursor(nextPage = 2, totalPages = 5, lastRefreshedAt = nowMillis), local.cursorOf(listKey))
    }

    @Test
    fun appendAsksForTheCursorsNextPageAndStoresItAfterPageOne() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(listKey, MOVIE_PAGE_SIZE, (1L..20L).map(::movie), ListCursor(2, 5, nowMillis))

        val result = mediator(local) { requestedPage ->
            assertEquals(2, requestedPage)
            Result.Success(page(2, 5, 21L..40L))
        }.load(LoadType.APPEND, emptyState)

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertEquals((1L..40L).toList(), local.positionsIn(listKey).map { it.second })
        assertEquals((0..39).toList(), local.positionsIn(listKey).map { it.first })
    }

    /**
     * Otherwise paging deep into a list would keep renewing the TTL and page one would never be
     * re-fetched, however long the session lasted.
     */
    @Test
    fun appendDoesNotRestartTheTtl() = runTest {
        val local = FakeMovieLocalDataSource()
        val firstRefresh = nowMillis - 1000
        local.replaceList(listKey, MOVIE_PAGE_SIZE, (1L..20L).map(::movie), ListCursor(2, 5, firstRefresh))

        mediator(local) { Result.Success(page(2, 5, 21L..40L)) }.load(LoadType.APPEND, emptyState)

        assertEquals(firstRefresh, local.cursorOf(listKey)?.lastRefreshedAt)
    }

    @Test
    fun appendWithNoCursorYetReportsEndOfPagination() = runTest {
        val result = mediator(FakeMovieLocalDataSource()) { error("must not fetch") }
            .load(LoadType.APPEND, emptyState)

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertTrue(result.endOfPaginationReached)
    }

    @Test
    fun appendStopsOnceTheServerHasNoNextPage() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(listKey, MOVIE_PAGE_SIZE, (1L..20L).map(::movie), ListCursor(null, 1, nowMillis))

        val result = mediator(local) { error("must not fetch") }.load(LoadType.APPEND, emptyState)

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertTrue(result.endOfPaginationReached)
    }

    @Test
    fun theLastPageClearsTheCursorAndEndsPagination() = runTest {
        val local = FakeMovieLocalDataSource()

        val result = mediator(local) { Result.Success(page(1, 1, 1L..20L)) }
            .load(LoadType.REFRESH, emptyState)

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertTrue(result.endOfPaginationReached)
        assertNull(local.cursorOf(listKey)?.nextPage)
    }

    @Test
    fun prependNeverFetches() = runTest {
        val result = mediator(FakeMovieLocalDataSource()) { error("must not fetch") }
            .load(LoadType.PREPEND, emptyState)

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertTrue(result.endOfPaginationReached)
    }

    @Test
    fun aFailedFetchLeavesTheCacheAloneAndSurfacesTheTypedError() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(listKey, MOVIE_PAGE_SIZE, (1L..20L).map(::movie), ListCursor(2, 5, nowMillis))
        val failure = MovieDataException.Network(RuntimeException("offline"))

        val result = mediator(local) { Result.Error(failure) }.load(LoadType.REFRESH, emptyState)

        assertIs<RemoteMediator.MediatorResult.Error>(result)
        assertEquals(failure, result.throwable)
        // The cached list survives a failed refresh, which is what lets the UI keep showing it.
        assertEquals((1L..20L).toList(), local.positionsIn(listKey).map { it.second })
    }

    @Test
    fun aPageThatOverlapsTheOneBeforeItDoesNotDuplicate() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(listKey, MOVIE_PAGE_SIZE, (1L..20L).map(::movie), ListCursor(2, 5, nowMillis))

        // TMDB repeats movie 20 on page two, as it does when the ordering shifts between calls.
        mediator(local) { Result.Success(page(2, 5, 20L..39L)) }.load(LoadType.APPEND, emptyState)

        val ids = local.positionsIn(listKey).map { it.second }
        assertEquals(ids.size, ids.distinct().size)
        assertEquals((1L..39L).toList(), ids)
        // Movie 20 kept the position it already had rather than moving down the list.
        assertEquals(19, local.positionsIn(listKey).first { it.second == 20L }.first)
    }
}
