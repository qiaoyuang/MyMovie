package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.similar.SimilarMoviesViewModel
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel.SimilarMoviesState.SUCCESS
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SimilarMovieViewModelTest : BasicTest() {

    @Test
    fun test_getSimilarMovies() = runTest {
        val viewModel = SimilarMoviesViewModel(MockedRepository(), 1L)
        viewModel.getSimilarMovies(false).join()
        assertEquals(5, (viewModel.movieState.value as? SUCCESS)?.value?.size)
        repeat(4) {
            viewModel.getSimilarMovies(true).join()
        }
        assertEquals(25, (viewModel.movieState.value as? SUCCESS)?.value?.size)
        viewModel.getSimilarMovies(true).join()
        assertEquals(25, (viewModel.movieState.value as? SUCCESS)?.value?.size)
    }
}