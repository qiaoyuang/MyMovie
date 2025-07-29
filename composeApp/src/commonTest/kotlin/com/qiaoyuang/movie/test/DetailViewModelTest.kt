package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.detail.DetailViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DetailViewModelTest : BasicTest() {

    @Test
    fun test_updateUI() = runTest {
        val viewModel = DetailViewModel(MockedRepository(), 1L)
        viewModel.updateUI().join()
        (viewModel.movieDetailState.value as? DetailViewModel.MovieDetailState.SUCCESS)?.let {
            assertEquals(1L, it.movie.id)
            assertEquals(('a'.code + it.movie.id.toInt()).toChar().toString(), it.movie.title)
            assertEquals("abc", it.movie.overview)
            assertEquals("https://xyz", it.movie.posterPath)
            assertEquals("https://uvw", it.movie.backdropPath)
            assertEquals(1.0.toString(), it.movie.voteAverage)
            assertEquals(1 % 3, it.movie.genreIds?.first())
            assertEquals(MockedRepository.COUNT,it.similarMovies?.size)
        } ?: throw IllegalStateException("Mocked API failed")
    }
}