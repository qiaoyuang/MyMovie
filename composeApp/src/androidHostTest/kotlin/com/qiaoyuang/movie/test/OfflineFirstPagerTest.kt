package com.qiaoyuang.movie.test

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import androidx.test.core.app.ApplicationProvider
import com.ctrip.sqllin.driver.deleteDatabase
import com.ctrip.sqllin.driver.toDatabasePath
import com.ctrip.sqllin.dsl.Database
import com.qiaoyuang.movie.model.MovieRepositoryImpl
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.local.CACHE_TTL_MILLIS
import com.qiaoyuang.movie.model.local.DATABASE_NAME
import com.qiaoyuang.movie.model.local.ListCursor
import com.qiaoyuang.movie.model.local.MovieLocalDataSourceImpl
import com.qiaoyuang.movie.model.local.TOP_RATED_LIST_KEY
import com.qiaoyuang.movie.model.local.createMovieDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The offline-first stack as the app assembles it — repository, mediator, sqllin's paging source
 * and a real SQLite database — with only the network faked. The mediator and the source only
 * work together through Paging: the source running out of rows is what asks the mediator to
 * fetch, and the mediator's write only reaches the list because SQLite's change counts invalidate
 * the source. Neither half's own tests can show that, so it is shown here, end to end.
 *
 * Every write re-presents a window around the user rather than the whole list, so full contents
 * are asserted against the cache, and the handshake is driven by Paging's own first fill.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstPagerTest {

    private val path = ApplicationProvider.getApplicationContext<Context>().toDatabasePath()
    private lateinit var database: Database
    private val nowMillis = 1_700_000_000_000L

    @Before
    fun open() {
        deleteDatabase(path, DATABASE_NAME)
        database = createMovieDatabase(path)
    }

    @After
    fun close() {
        database.close()
        deleteDatabase(path, DATABASE_NAME)
    }

    private class Stack(
        val repository: MovieRepositoryImpl,
        val local: MovieLocalDataSourceImpl,
        val service: FakeApiService,
    )

    /** Everything on the test scheduler, so the database work and Paging's own run in step. */
    private fun TestScope.stack(service: FakeApiService = FakeApiService(), now: Long = nowMillis): Stack {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val local = MovieLocalDataSourceImpl(database, dispatcher)
        return Stack(MovieRepositoryImpl(service, local, dispatcher, now = { now }), local, service)
    }

    private fun movie(id: Long, title: String = "cached $id") =
        Movie(id, title, "overview $id", "/poster$id", null, 7.0, listOf(28, 12))

    private suspend fun Stack.cachedTopRatedIds() =
        local.moviesIn(TOP_RATED_LIST_KEY, limit = 1000, offset = 0).map(Movie::id)

    // ---- the handshake ----

    /**
     * The mediator's refresh brings in page one, which is short of the window Paging wants, so the
     * source runs out of rows and the mediator is asked to append — more than one page requested
     * is that loop working.
     */
    @Test
    fun aColdCacheFetchesFromPageOneAndCachesWhatItGets() = runTest {
        val s = stack()

        val items = s.repository.pagedTopRatedMovies().asSnapshot()

        val requested = s.service.topRatedPagesRequested
        assertEquals(1, requested.first())
        assertTrue(requested.size >= 2, "the source running out should have asked for an append")
        assertEquals(requested.sorted().distinct(), requested)
        assertEquals((1L..requested.size * 20L).toList(), s.cachedTopRatedIds())
        assertEquals(items.map(Movie::id).distinct(), items.map(Movie::id))
    }

    @Test
    fun theListStopsWhenTheServerRunsOut() = runTest {
        val s = stack(FakeApiService().apply { topRatedTotalPages = 2 })

        s.repository.pagedTopRatedMovies().asSnapshot()

        assertEquals(listOf(1, 2), s.service.topRatedPagesRequested)
        assertNull(s.local.cursorOf(TOP_RATED_LIST_KEY)?.nextPage)
        assertEquals((1L..40L).toList(), s.cachedTopRatedIds())
    }

    // ---- what the TTL decides ----

    @Test
    fun aFreshCacheIsNotRefetched() = runTest {
        val s = stack(now = nowMillis + CACHE_TTL_MILLIS - 1)
        s.local.replaceList(TOP_RATED_LIST_KEY, 20, (1L..20L).map { movie(it) }, ListCursor(2, 4, nowMillis))

        s.repository.pagedTopRatedMovies().asSnapshot()

        assertFalse(1 in s.service.topRatedPagesRequested, "a fresh cache must not re-read page one")
    }

    @Test
    fun aStaleCacheIsRefreshedAndItsOldRowsDiscarded() = runTest {
        val s = stack(now = nowMillis + CACHE_TTL_MILLIS)
        s.local.replaceList(TOP_RATED_LIST_KEY, 20, listOf(movie(999L)), ListCursor(2, 4, nowMillis))

        s.repository.pagedTopRatedMovies().asSnapshot()

        assertEquals(1, s.service.topRatedPagesRequested.first())
        assertFalse(999L in s.cachedTopRatedIds(), "the stale movie should be gone")
    }

    /** The point of the whole exercise: built entirely from the cache, with the network down. */
    @Test
    fun aFullyCachedListNeedsNoNetworkAtAll() = runTest {
        val s = stack(FakeApiService().apply { offline = true })
        s.local.replaceList(TOP_RATED_LIST_KEY, 20, (1L..20L).map { movie(it) }, ListCursor(null, 1, nowMillis))

        val items = s.repository.pagedTopRatedMovies().asSnapshot()

        assertEquals(emptyList(), s.service.topRatedPagesRequested)
        assertEquals((1L..20L).toList(), items.map(Movie::id))
        // And it is the domain model that comes out, genre ids in TMDB's order included.
        assertEquals(listOf(28, 12), items.first().genreIds)
    }

    @Test
    fun aPageThatRepeatsTheOneBeforeItIsNotPresentedTwice() = runTest {
        // Every page repeats its predecessor's last movie, as TMDB does when its ordering shifts.
        val service = FakeApiService().apply {
            topRatedIds = { page ->
                val first = (page - 1L) * 20 + 1 - (page - 1)
                (first until first + 20).toList()
            }
        }
        val s = stack(service)

        val items = s.repository.pagedTopRatedMovies().asSnapshot()

        val cached = s.cachedTopRatedIds()
        assertEquals(cached.distinct(), cached)
        assertEquals(items.map(Movie::id).distinct(), items.map(Movie::id))
    }

    /**
     * The detail strip fills the cache through the repository, and the paged screen's mediator
     * then opens on a fresh cursor and continues from page two instead of re-reading page one.
     */
    @Test
    fun theDetailStripAndThePagedScreenShareOneFetch() = runTest {
        val s = stack()

        s.repository.similarMoviesFirstPage(550L)
        assertEquals(listOf(1), s.service.similarPagesRequested)

        s.repository.pagedSimilarMovies(550L).asSnapshot()

        assertEquals(listOf(1, 2, 3), s.service.similarPagesRequested)
    }

    // ---- what invalidates a source ----

    private suspend fun Stack.loadedTopRatedSource(): PagingSource<Int, *> {
        local.replaceList(TOP_RATED_LIST_KEY, 20, (1L..20L).map { movie(it) }, ListCursor(2, 4, nowMillis))
        // The source registers for changes from its first page, so it has to have loaded one.
        return local.pagingSource(TOP_RATED_LIST_KEY).also { source ->
            source.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
            assertFalse(source.invalid)
        }
    }

    @Test
    fun anAppendedPageInvalidatesTheList() = runTest {
        val s = stack()
        val source = s.loadedTopRatedSource()

        s.local.appendPage(TOP_RATED_LIST_KEY, page = 2, pageSize = 20, (21L..40L).map { movie(it) }, ListCursor(3, 4, nowMillis))

        assertTrue(source.invalid)
    }

    /**
     * New with the library's source. The hand-written tracker this replaced was told about list
     * writes only, so a movie re-fetched for the detail screen left the list showing its old
     * title until the list itself was refreshed. The page is one SELECT over both tables now, so
     * a write to either one invalidates it.
     */
    @Test
    fun aRefreshedMovieInvalidatesTheListsThatShowIt() = runTest {
        val s = stack()
        val source = s.loadedTopRatedSource()

        s.local.upsertMovies(listOf(movie(5L, title = "renamed")))

        assertTrue(source.invalid)
    }

    /**
     * Tables, not rows: a write to another list shares the table, so it invalidates this one too.
     * Accepted — the cost is one reload of the window, which the presenter then diffs to nothing.
     */
    @Test
    fun aWriteToAnotherListAlsoInvalidates() = runTest {
        val s = stack()
        val source = s.loadedTopRatedSource()

        s.local.replaceList("similar:550", 20, listOf(movie(77L)), ListCursor(null, 1, nowMillis))

        assertTrue(source.invalid)
    }

    @Test
    fun aTableTheListDoesNotReadLeavesItAlone() = runTest {
        val s = stack()
        val source = s.loadedTopRatedSource()

        s.local.replaceGenres(listOf(MovieGenre(28, "Action")), refreshedAt = nowMillis)

        assertFalse(source.invalid)
    }
}
