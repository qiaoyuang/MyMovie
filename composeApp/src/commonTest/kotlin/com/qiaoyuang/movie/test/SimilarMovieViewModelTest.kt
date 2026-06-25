package com.qiaoyuang.movie.test

import app.cash.turbine.test
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel.SimilarMoviesState.SUCCESS
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimilarMovieViewModelTest : BasicTest() {

    @Test
    fun test_getSimilarMovies() = runTest {
        val viewModel = SimilarMoviesViewModel(MockedRepository(), 1L)
        viewModel.movieState.test {
            val (list1, isNoMore1) = awaitItem().convert2SUCCESS()
            assertEquals(emptyList(), list1)
            assertEquals(false, isNoMore1)
            viewModel.getSimilarMovies()
            val (list2, isNoMore2) = awaitItem().convert2SUCCESS()
            assertEquals(5, list2.size)
            assertTrue(isNoMore2)
            repeat(4) {
                viewModel.getSimilarMovies()
            }
        }
        assertEquals(5, (viewModel.movieState.value as? SUCCESS)?.data?.size)
        repeat(4) {
            viewModel.getSimilarMovies().join()
        }
        assertEquals(25, (viewModel.movieState.value as? SUCCESS)?.data?.size)
        viewModel.getSimilarMovies().join()
        assertEquals(25, (viewModel.movieState.value as? SUCCESS)?.data?.size)
    }

    private fun SimilarMoviesViewModel.SimilarMoviesState.convert2SUCCESS(): SUCCESS =
        this as? SUCCESS ?: throw IllegalStateException("Not expectation")
}