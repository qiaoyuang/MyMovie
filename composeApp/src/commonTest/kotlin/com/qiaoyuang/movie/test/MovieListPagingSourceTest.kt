package com.qiaoyuang.movie.test

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.qiaoyuang.movie.model.MOVIE_PAGE_SIZE
import com.qiaoyuang.movie.model.MovieListPagingSource
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.local.ListCursor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class) // runCurrent()
class MovieListPagingSourceTest {

    private val listKey = "top_rated"

    private fun movie(id: Long) = Movie(
        id = id,
        title = "title$id",
        overview = "overview$id",
        posterPath = null,
        backdropPath = null,
        voteAverage = 8.0,
        genreIds = null,
    )

    private suspend fun FakeMovieLocalDataSource.seed(ids: LongRange) =
        replaceList(listKey, MOVIE_PAGE_SIZE, ids.map(::movie), ListCursor(2, 5, 0L))

    private fun refresh(loadSize: Int = MOVIE_PAGE_SIZE, key: Int? = null) =
        PagingSource.LoadParams.Refresh<Int>(key = key, loadSize = loadSize, placeholdersEnabled = false)

    private fun append(key: Int, loadSize: Int = MOVIE_PAGE_SIZE) =
        PagingSource.LoadParams.Append(key = key, loadSize = loadSize, placeholdersEnabled = false)

    @Test
    fun anEmptyCacheLoadsAnEmptyPageAndAsksForNoMore() = runTest {
        val source = MovieListPagingSource(FakeMovieLocalDataSource(), listKey, TestScope())

        val result = source.load(refresh())

        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(result)
        assertEquals(emptyList(), result.data)
        // Not "the list ended" — this is what makes Paging ask the mediator to fetch page one.
        assertNull(result.nextKey)
    }

    @Test
    fun aFullPageOffersTheNextOffset() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..40L)
        val source = MovieListPagingSource(local, listKey, TestScope())

        val result = source.load(refresh())

        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(result)
        assertEquals((1L..20L).toList(), result.data.map(Movie::id))
        assertEquals(20, result.nextKey)
        // The top of the list has nothing above it.
        assertNull(result.prevKey)
    }

    @Test
    fun aPageBelowTheTopCanBePagedBackTo() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..60L)
        val source = MovieListPagingSource(local, listKey, TestScope())

        val result = source.load(append(key = 40))

        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(result)
        // Exactly one page above, so a prepend neither overlaps nor skips.
        assertEquals(20, result.prevKey)
    }

    @Test
    fun prependReadsThePageAboveWithoutOverlapping() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..60L)
        val source = MovieListPagingSource(local, listKey, TestScope())

        val lower = source.load(append(key = 40))
        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(lower)
        val upper = source.load(
            PagingSource.LoadParams.Prepend(key = lower.prevKey!!, loadSize = MOVIE_PAGE_SIZE, placeholdersEnabled = false)
        )

        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(upper)
        assertEquals((21L..40L).toList(), upper.data.map(Movie::id))
        assertEquals((41L..60L).toList(), lower.data.map(Movie::id))
    }

    @Test
    fun readsResumeFromTheOffsetKeyInStoredOrder() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..40L)
        val source = MovieListPagingSource(local, listKey, TestScope())

        val result = source.load(append(key = 20))

        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(result)
        assertEquals((21L..40L).toList(), result.data.map(Movie::id))
    }

    @Test
    fun aShortPageStopsTheSourceSoTheMediatorTakesOver() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..30L)
        val source = MovieListPagingSource(local, listKey, TestScope())

        val result = source.load(append(key = 20))

        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(result)
        assertEquals((21L..30L).toList(), result.data.map(Movie::id))
        assertNull(result.nextKey)
    }

    @Test
    fun aWriteInvalidatesTheSource() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..20L)
        val source = MovieListPagingSource(local, listKey, backgroundScope)
        source.load(refresh())
        assertFalse(source.invalid)

        local.appendPage(listKey, page = 2, MOVIE_PAGE_SIZE, (21L..40L).map(::movie), ListCursor(3, 5, 0L))
        runCurrent()

        assertTrue(source.invalid)
    }

    @Test
    fun aWriteToAnotherListLeavesThisSourceValid() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..20L)
        val source = MovieListPagingSource(local, listKey, backgroundScope)
        source.load(refresh())

        local.replaceList("similar:550", MOVIE_PAGE_SIZE, listOf(movie(99L)), ListCursor(null, 1, 0L))
        runCurrent()

        assertFalse(source.invalid)
    }

    /**
     * The reason the tracker is version-based: this write lands after the source is constructed
     * but before its collector runs, and must not be lost.
     */
    @Test
    fun aWriteBeforeTheCollectorStartsStillInvalidates() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..20L)
        val source = MovieListPagingSource(local, listKey, backgroundScope)

        local.appendPage(listKey, page = 2, MOVIE_PAGE_SIZE, (21L..40L).map(::movie), ListCursor(3, 5, 0L))
        runCurrent()

        assertTrue(source.invalid)
    }

    @Test
    fun refreshResumesAroundTheAnchorRatherThanAtTheTop() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..60L)
        val source = MovieListPagingSource(local, listKey, TestScope())
        val page = source.load(append(key = 40))
        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(page)

        // The page's prevKey says the presented list starts at offset 40, so anchor 5 is really
        // offset 45, which snaps down to the page boundary at 40.
        val key = source.getRefreshKey(
            PagingState(
                pages = listOf(page),
                anchorPosition = 5,
                config = PagingConfig(pageSize = MOVIE_PAGE_SIZE),
                leadingPlaceholderCount = 0,
            )
        )

        assertEquals(40, key)
    }

    @Test
    fun refreshWithNoAnchorStartsAtTheTop() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..20L)
        val source = MovieListPagingSource(local, listKey, TestScope())

        val key = source.getRefreshKey(
            PagingState(
                pages = emptyList(),
                anchorPosition = null,
                config = PagingConfig(pageSize = MOVIE_PAGE_SIZE),
                leadingPlaceholderCount = 0,
            )
        )

        assertNull(key)
    }

    @Test
    fun refreshNeverReturnsANegativeOffset() = runTest {
        val local = FakeMovieLocalDataSource()
        local.seed(1L..20L)
        val source = MovieListPagingSource(local, listKey, TestScope())
        val page = source.load(refresh())
        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(page)

        val key = source.getRefreshKey(
            PagingState(
                pages = listOf(page),
                anchorPosition = 0,
                config = PagingConfig(pageSize = MOVIE_PAGE_SIZE),
                leadingPlaceholderCount = 0,
            )
        )

        assertEquals(0, key)
    }
}
