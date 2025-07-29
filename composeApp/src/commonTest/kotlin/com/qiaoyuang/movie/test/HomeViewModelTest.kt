package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.home.HomeViewModel
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.SHOW
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : BasicTest() {

    @Test
    fun test_getMovies() = runTest {
        val viewModel = HomeViewModel(MockedRepository())
        viewModel.getTopMovies(false).join()
        assertEquals(5, (viewModel.movieState.value as? SHOW)?.value?.size)
        repeat(4) {
            viewModel.getTopMovies(true).join()
        }
        assertEquals(25, (viewModel.movieState.value as? SHOW)?.value?.size)
        viewModel.getTopMovies(true).join()
        assertEquals(25, (viewModel.movieState.value as? SHOW)?.value?.size)
    }
}