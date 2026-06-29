package com.qiaoyuang.movie.test

import app.cash.turbine.test
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimilarMovieViewModelTest : BasicTest() {

    @Test
    fun test_getSimilarMovies() = runTest {
        val viewModel = SimilarMoviesViewModel(MockedRepository(), 1L)
        viewModel.movieState.test {
            val state1 = awaitItem()
            assertEquals(emptyList(), state1.data)
            assertEquals(false, state1.isNoMore)
            assertEquals(false, state1.isLoading)
            assertEquals(false, state1.isError)
            viewModel.getSimilarMovies()
            val state2 = awaitItem()
            assertEquals(emptyList(), state2.data)
            assertTrue(state2.isLoading)
            val state3 = awaitItem()
            assertEquals(5, state3.data.size)
            assertEquals(false, state3.isNoMore)
            assertEquals(false, state3.isLoading)
            repeat(4) {
                viewModel.getSimilarMovies()
            }
            skipItems(7)
            val state4 = awaitItem()
            assertEquals(25, state4.data.size)
            assertEquals(false, state4.isNoMore)
            viewModel.getSimilarMovies()
            val state5 = awaitItem()
            assertEquals(25, state5.data.size)
            assertEquals(true, state5.isNoMore)
            cancelAndIgnoreRemainingEvents()
        }
    }
}