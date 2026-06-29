package com.qiaoyuang.movie.test

import app.cash.turbine.test
import com.qiaoyuang.movie.detail.DetailViewModel
import com.qiaoyuang.movie.domain.SimilarMovieUseCaseImpl
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DetailViewModelTest : BasicTest() {

    private fun detailViewModel(repository: MovieRepository) = DetailViewModel(
        repository,
        SimilarMovieUseCaseImpl(repository, mainThreadSurrogate, 1L),
        1L,
    )

    @Test
    fun test_updateUI() = runTest {
        val viewModel = detailViewModel(MockedRepository())
        viewModel.movieDetailState.test {
            assertIs<DetailViewModel.MovieDetailState.LOADING>(awaitItem())
            viewModel.updateUI()
            val success = assertIs<DetailViewModel.MovieDetailState.SUCCESS>(awaitItem())
            assertEquals(1L, success.movie.id)
            assertEquals(('a'.code + success.movie.id.toInt()).toChar().toString(), success.movie.title)
            assertEquals("abc", success.movie.overview)
            assertEquals("https://xyz", success.movie.posterPath)
            assertEquals("https://uvw", success.movie.backdropPath)
            assertEquals(1.0.toString(), success.movie.voteAverage)
            assertEquals(1 % 3, success.movie.genreIds?.first())
            assertEquals(MockedRepository.COUNT, success.similarMovies?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_updateUI_error() = runTest {
        val viewModel = detailViewModel(ErrorMockedRepository())
        viewModel.movieDetailState.test {
            assertIs<DetailViewModel.MovieDetailState.LOADING>(awaitItem())
            viewModel.updateUI()
            val error = assertIs<DetailViewModel.MovieDetailState.ERROR>(awaitItem())
            assertEquals(ErrorMockedRepository.ERROR_MESSAGE, error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
