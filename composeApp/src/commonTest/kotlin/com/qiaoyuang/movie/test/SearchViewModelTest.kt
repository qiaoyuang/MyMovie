package com.qiaoyuang.movie.test

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.search.SearchViewModel
import com.qiaoyuang.movie.search.matchesGenres
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchViewModelTest : BasicTest() {

    private fun searchViewModel(repository: MovieRepository = MockedRepository()) =
        SearchViewModel(repository, SavedStateHandle())

    private fun movie(genreIds: List<Int>?) = Movie(
        id = 1L,
        title = "t",
        overview = "o",
        posterPath = null,
        backdropPath = null,
        voteAverage = null,
        genreIds = genreIds,
    )

    @Test
    fun test_empty_selection_matches_everything() {
        assertTrue(movie(listOf(7)).matchesGenres(emptySet()))
        assertTrue(movie(null).matchesGenres(emptySet()))
    }

    @Test
    fun test_any_overlap_matches() {
        assertTrue(movie(listOf(1, 5)).matchesGenres(setOf(5, 9)))
        assertFalse(movie(listOf(1, 5)).matchesGenres(setOf(2, 9)))
    }

    @Test
    fun test_movie_without_genres_never_survives_a_selection() {
        assertFalse(movie(null).matchesGenres(setOf(1)))
        assertFalse(movie(emptyList()).matchesGenres(setOf(1)))
    }

    @Test
    fun test_toggleGenre_flips_the_selection() = runTest {
        val viewModel = searchViewModel()
        viewModel.genreFilterState.test {
            assertEquals(emptySet(), awaitItem().selectedIds)
            viewModel.toggleGenre(2)
            assertEquals(setOf(2), awaitItem().selectedIds)
            viewModel.toggleGenre(3)
            assertEquals(setOf(2, 3), awaitItem().selectedIds)
            viewModel.toggleGenre(2)
            assertEquals(setOf(3), awaitItem().selectedIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_prepareGenreList() = runTest {
        val viewModel = searchViewModel()
        viewModel.genreFilterState.test {
            assertTrue(awaitItem().genres.isEmpty())
            viewModel.prepareGenreList()?.join()
            assertEquals(MockedRepository.GENRE_SIZE, awaitItem().genres.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_prepareGenreList_error() = runTest {
        val viewModel = searchViewModel(ErrorMockedRepository())
        viewModel.genreFilterState.test {
            assertTrue(awaitItem().genres.isEmpty())
            viewModel.prepareGenreList()?.join()
            // A failed fetch leaves the catalogue empty, so no new state is emitted
            expectNoEvents()
        }
    }
}
