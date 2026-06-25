package com.qiaoyuang.movie.test

import app.cash.turbine.test
import com.qiaoyuang.movie.home.HomeViewModel
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.SUCCESS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : BasicTest() {

    @Test
    fun test_getMovies() = runTest {
        val viewModel = HomeViewModel(MockedRepository(), mainThreadSurrogate)
        viewModel.movieState.test {
            assertEquals(true, (awaitItem() as? SUCCESS)?.data?.isEmpty())
            viewModel.getTopMovies()
            assertEquals(true, awaitItem() is HomeViewModel.TopMoviesState.LOADING)
            assertEquals(5, (awaitItem() as? SUCCESS)?.data?.size)
            repeat(4) {
                viewModel.getTopMovies()
            }
            skipItems(7)
            assertEquals(25, (awaitItem() as? SUCCESS)?.data?.size)
            viewModel.getTopMovies()
            assertEquals(25, (awaitItem() as? SUCCESS)?.data?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}