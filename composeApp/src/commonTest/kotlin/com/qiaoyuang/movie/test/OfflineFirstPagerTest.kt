package com.qiaoyuang.movie.test

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.testing.asSnapshot
import com.qiaoyuang.movie.model.MOVIE_PAGE_SIZE
import com.qiaoyuang.movie.model.MovieDataException
import com.qiaoyuang.movie.model.MovieListPagingSource
import com.qiaoyuang.movie.model.MovieRemoteMediator
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.local.CACHE_TTL_MILLIS
import com.qiaoyuang.movie.model.local.ListCursor
import com.qiaoyuang.movie.model.local.TOP_RATED_LIST_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The mediator and the PagingSource only work together through Paging: the source reporting no
 * next key is what asks the mediator to fetch, and the mediator's write is only seen because the
 * invalidation tracker restarts the source. Neither half's unit tests can show that handshake,
 * so this assembles the same Pager HomeViewModel builds and checks what actually happens.
 *
 * Two things shape the assertions. Every mediator write invalidates the source, so Paging
 * re-presents a window around the user rather than the whole list — the full contents are
 * therefore asserted against the cache, and the handshake is driven by Paging's own initial fill
 * rather than by simulated scrolling (asSnapshot's scroll helpers assume a list that only grows,
 * which a re-windowing source is not). And asSnapshot abandons the snapshot as soon as a load
 * reports an error, so "a failed refresh keeps showing cached movies" is covered by
 * MovieRemoteMediatorTest (the cache survives it) plus MovieListPagingSourceTest (it is still
 * served) instead of here.
 */
@OptIn(ExperimentalPagingApi::class)
class OfflineFirstPagerTest {

    private val nowMillis = 1_700_000_000_000L

    /** Paging's default first load is three pages, so a cold start fills 60 items. */
    private val initialWindow = 3 * MOVIE_PAGE_SIZE

    private fun movie(id: Long) = Movie(
        id = id,
        title = "title$id",
        overview = "overview$id",
        posterPath = null,
        backdropPath = null,
        voteAverage = 8.0,
        genreIds = null,
    )

    /** Page n holds ids (n-1)*20+1 .. n*20, like a well-behaved TMDB list. */
    private fun serverPage(page: Int, totalPages: Int) = MovieResponse(
        page = page,
        results = ((page - 1L) * MOVIE_PAGE_SIZE + 1..page.toLong() * MOVIE_PAGE_SIZE).map(::movie),
        totalPages = totalPages,
    )

    private fun pagerFlow(
        local: FakeMovieLocalDataSource,
        scope: CoroutineScope,
        now: Long = nowMillis,
        fetchPage: suspend (page: Int) -> Result<MovieResponse, MovieDataException>,
    ) = Pager(
        config = PagingConfig(pageSize = MOVIE_PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = MovieRemoteMediator(TOP_RATED_LIST_KEY, local, fetchPage, now = { now }),
        pagingSourceFactory = { MovieListPagingSource(local, TOP_RATED_LIST_KEY, scope) },
    ).flow

    private fun FakeMovieLocalDataSource.cachedIds() =
        positionsIn(TOP_RATED_LIST_KEY).map { it.second }

    /**
     * Also the handshake test. The mediator's refresh only brings in page one, which is short of
     * the window Paging wants — so the source runs dry, reports no next key, and the mediator is
     * asked to append. Pages two and three appearing is that loop working three times over.
     */
    @Test
    fun aColdCacheFetchesFromPageOneAndCachesEverythingItGets() = runTest {
        val local = FakeMovieLocalDataSource()
        val requested = mutableListOf<Int>()

        val items = pagerFlow(local, backgroundScope) { page ->
            requested += page
            Result.Success(serverPage(page, totalPages = 4))
        }.asSnapshot()

        assertEquals(listOf(1, 2, 3), requested)
        assertEquals((1L..initialWindow.toLong()).toList(), local.cachedIds())
        // Contiguous positions, so nothing was written twice or skipped.
        assertEquals((0 until initialWindow).toList(), local.positionsIn(TOP_RATED_LIST_KEY).map { it.first })
        assertEquals(4, local.cursorOf(TOP_RATED_LIST_KEY)?.nextPage)
        assertTrue(items.isNotEmpty())
        assertEquals(items.map(Movie::id).distinct(), items.map(Movie::id))
    }

    @Test
    fun theListStopsWhenTheServerRunsOut() = runTest {
        val local = FakeMovieLocalDataSource()
        val requested = mutableListOf<Int>()

        pagerFlow(local, backgroundScope) { page ->
            requested += page
            Result.Success(serverPage(page, totalPages = 2))
        }.asSnapshot()

        // Page three is never asked for, because the cursor said page two was the last.
        assertEquals(listOf(1, 2), requested)
        assertNull(local.cursorOf(TOP_RATED_LIST_KEY)?.nextPage)
        assertEquals((1L..40L).toList(), local.cachedIds())
    }

    /**
     * SKIP_INITIAL_REFRESH means page one is not re-read. Paging still appends past the end of
     * the cache, which is the point of keeping a cursor — so what matters is that nothing
     * re-fetches what is already stored.
     */
    @Test
    fun aFreshCacheIsNotRefetched() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(
            TOP_RATED_LIST_KEY,
            MOVIE_PAGE_SIZE,
            (1L..20L).map(::movie),
            ListCursor(nextPage = 2, totalPages = 4, lastRefreshedAt = nowMillis),
        )
        val requested = mutableListOf<Int>()

        pagerFlow(local, backgroundScope, now = nowMillis + CACHE_TTL_MILLIS - 1) { page ->
            requested += page
            Result.Success(serverPage(page, totalPages = 4))
        }.asSnapshot()

        assertFalse(1 in requested, "a fresh cache must not re-read page one")
        assertEquals(2, requested.first())
    }

    @Test
    fun aStaleCacheIsRefreshedAndItsOldRowsDiscarded() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(
            TOP_RATED_LIST_KEY,
            MOVIE_PAGE_SIZE,
            listOf(movie(999L)),
            ListCursor(nextPage = 2, totalPages = 4, lastRefreshedAt = nowMillis),
        )
        val requested = mutableListOf<Int>()

        pagerFlow(local, backgroundScope, now = nowMillis + CACHE_TTL_MILLIS) { page ->
            requested += page
            Result.Success(serverPage(page, totalPages = 4))
        }.asSnapshot()

        assertEquals(1, requested.first(), "a stale cache must re-read page one")
        assertFalse(999L in local.cachedIds(), "the stale movie should be gone")
    }

    /**
     * The point of the whole exercise. Fresh, and with no next page, so there is nothing to
     * refresh and nothing to append: the list is built entirely from the cache, and the fetch
     * lambda fails the test if Paging reaches for the network at all.
     */
    @Test
    fun aFullyCachedListNeedsNoNetworkAtAll() = runTest {
        val local = FakeMovieLocalDataSource()
        local.replaceList(
            TOP_RATED_LIST_KEY,
            MOVIE_PAGE_SIZE,
            (1L..20L).map(::movie),
            ListCursor(nextPage = null, totalPages = 1, lastRefreshedAt = nowMillis),
        )

        val items = pagerFlow(local, backgroundScope) { error("the network must not be touched") }
            .asSnapshot()

        assertEquals((1L..20L).toList(), items.map(Movie::id))
    }

    @Test
    fun aPageThatOverlapsTheOneBeforeItIsNotPresentedTwice() = runTest {
        val local = FakeMovieLocalDataSource()

        // Every page repeats its predecessor's last movie, as TMDB does when the ordering shifts.
        val items = pagerFlow(local, backgroundScope) { page ->
            val start = (page - 1L) * MOVIE_PAGE_SIZE + 1 - (page - 1)
            Result.Success(
                MovieResponse(
                    page = page,
                    results = (start until start + MOVIE_PAGE_SIZE).map(::movie),
                    totalPages = 4,
                )
            )
        }.asSnapshot()

        val cached = local.cachedIds()
        assertEquals(cached.distinct(), cached)
        assertEquals(items.map(Movie::id).distinct(), items.map(Movie::id))
    }
}
