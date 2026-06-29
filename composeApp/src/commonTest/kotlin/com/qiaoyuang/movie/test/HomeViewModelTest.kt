package com.qiaoyuang.movie.test

import app.cash.turbine.test
import com.qiaoyuang.movie.home.HomeViewModel
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.ERROR
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.LOADING
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.SUCCESS
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : BasicTest() {

    private fun homeViewModel(repository: MovieRepository = MockedRepository()) =
        HomeViewModel(repository, mainThreadSurrogate)

    @Test
    fun test_getMovies() = runTest {
        val viewModel = homeViewModel()
        viewModel.movieState.test {
            assertTrue(assertIs<SUCCESS>(awaitItem()).data.isEmpty())
            viewModel.getTopMovies()
            assertIs<LOADING>(awaitItem())
            assertEquals(MockedRepository.COUNT, assertIs<SUCCESS>(awaitItem()).data.size)
            repeat(4) {
                viewModel.getTopMovies()
            }
            // 4 calls × (LOADING + SUCCESS) − 1, leaving the final SUCCESS to await below
            skipItems(2 * 4 - 1)
            val total = MockedRepository.COUNT * MockedRepository.TOTAL_PAGES
            assertEquals(total, assertIs<SUCCESS>(awaitItem()).data.size)
            viewModel.getTopMovies()
            assertEquals(total, assertIs<SUCCESS>(awaitItem()).data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_getMovies_error() = runTest {
        val viewModel = homeViewModel(ErrorMockedRepository())
        viewModel.movieState.test {
            assertTrue(assertIs<SUCCESS>(awaitItem()).data.isEmpty())
            viewModel.getTopMovies()
            assertIs<LOADING>(awaitItem())
            val error = assertIs<ERROR>(awaitItem())
            assertTrue(error.data.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
