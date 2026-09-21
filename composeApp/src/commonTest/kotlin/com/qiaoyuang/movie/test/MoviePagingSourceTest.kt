package com.qiaoyuang.movie.test

import androidx.paging.PagingSource
import com.qiaoyuang.movie.model.MoviePagingSource
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Replaces HomeViewModelTest and SimilarMovieViewModelTest. With the pagination state machine
 * inside Paging, the thing worth asserting is how an endpoint maps onto LoadResult — which
 * needs no ViewModel, no Main dispatcher and no Turbine.
 */
class MoviePagingSourceTest {

    private fun topRated(repository: MovieRepository = MockedRepository()) =
        MoviePagingSource { page -> repository.fetchTopRated(page) }

    private fun similar(repository: MovieRepository = MockedRepository()) =
        MoviePagingSource { page -> repository.similarMovies(MOVIE_ID, page) }

    private fun refresh(key: Int? = null) = PagingSource.LoadParams.Refresh(
        key = key,
        loadSize = MockedRepository.COUNT,
        placeholdersEnabled = false,
    )

    private fun append(key: Int) = PagingSource.LoadParams.Append(
        key = key,
        loadSize = MockedRepository.COUNT,
        placeholdersEnabled = false,
    )

    private suspend fun MoviePagingSource.loadPage(params: PagingSource.LoadParams<Int>) =
        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(load(params))

    @Test
    fun test_first_page() = runTest {
        val page = topRated().loadPage(refresh())
        assertEquals(MockedRepository.COUNT, page.data.size)
        // prevKey stays null so Paging never asks for a Prepend.
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun test_middle_page_keeps_appending() = runTest {
        assertEquals(3, topRated().loadPage(append(2)).nextKey)
    }

    @Test
    fun test_last_page_ends_pagination() = runTest {
        // A null nextKey is how endOfPaginationReached becomes true in the UI.
        assertNull(topRated().loadPage(append(MockedRepository.TOTAL_PAGES)).nextKey)
    }

    @Test
    fun test_error_is_reported_as_load_error() = runTest {
        val result = topRated(ErrorMockedRepository()).load(refresh())
        val error = assertIs<PagingSource.LoadResult.Error<Int, Movie>>(result)
        // Passed through as-is: no MovieLoadException wrapper hiding what kind of failure it was.
        assertSame(ErrorMockedRepository.ERROR, error.throwable)
    }

    @Test
    fun test_similar_movies_endpoint() = runTest {
        val page = similar().loadPage(refresh())
        assertEquals(MockedRepository.COUNT, page.data.size)
        assertEquals(2, page.nextKey)
        assertNull(similar().loadPage(append(MockedRepository.TOTAL_PAGES)).nextKey)
    }

    /**
     * Regression test: nextKey used to be derived from the page the response echoes back.
     * MockedRepository.search reported page 1 for every request, so every page produced
     * nextKey = 2 and Paging aborted with "the same value was passed as the nextKey in two
     * sequential Pages". Deriving it from the requested key keeps the keys monotonic.
     */
    @Test
    fun test_next_key_follows_the_requested_page() = runTest {
        val source = MoviePagingSource { page ->
            // A server that always claims to be on page 1, whatever was asked for.
            MockedRepository().fetchTopRated(page).let { result ->
                assertIs<com.qiaoyuang.movie.model.Result.Success<MovieResponse>>(result)
                com.qiaoyuang.movie.model.Result.Success(result.data.copy(page = 1))
            }
        }
        assertEquals(3, source.loadPage(append(2)).nextKey)
        assertEquals(5, source.loadPage(append(4)).nextKey)
    }

    @Test
    fun test_similar_movies_error() = runTest {
        val result = similar(ErrorMockedRepository()).load(refresh())
        val error = assertIs<PagingSource.LoadResult.Error<Int, Movie>>(result)
        // Passed through as-is: no MovieLoadException wrapper hiding what kind of failure it was.
        assertSame(ErrorMockedRepository.ERROR, error.throwable)
    }

    private companion object {
        const val MOVIE_ID = 1L
    }
}
