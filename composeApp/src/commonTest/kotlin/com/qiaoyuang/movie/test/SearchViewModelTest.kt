package com.qiaoyuang.movie.test

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.search.SearchViewModel
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.ERROR
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.LOADING
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.SUCCESS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest : BasicTest() {

    private fun searchViewModel(repository: MovieRepository = MockedRepository()) =
        SearchViewModel(repository, SavedStateHandle(), mainThreadSurrogate)

    private val viewModel = searchViewModel()

    @Test
    fun test_prepareGenreList() = runTest {
        viewModel.prepareGenreList()?.join()
        assertEquals(MockedRepository.GENRE_SIZE, viewModel.showGenreList.value.size)
    }

    @Test
    fun test_search() = runTest {
        viewModel.finalResultFlow.test {
            val initial = awaitItem()
            assertEquals(emptyList(), initial.data)
            assertTrue(initial.state is SUCCESS)

            viewModel.search("movie")
            advanceTimeBy(299.toDuration(DurationUnit.MILLISECONDS))
            expectNoEvents()
            val loading1 = awaitItem()
            assertEquals(emptyList(), loading1.data)
            assertTrue(loading1.state is LOADING)

            val success1 = awaitItem()
            assertEquals(MockedRepository.TOTAL_RESULTS, success1.data.size)
            assertFalse(assertIs<SUCCESS>(success1.state).isNoMore)

            // Load pages 2–4 one at a time so the scan accumulates correctly
            repeat(3) {
                viewModel.loadMore()
                skipItems(2)
            }

            // Load the last page and verify isNoMore
            viewModel.loadMore()
            val loading5 = awaitItem()
            assertTrue(loading5.state is LOADING)
            val success5 = awaitItem()
            assertEquals(MockedRepository.TOTAL_RESULTS * MockedRepository.TOTAL_PAGES, success5.data.size)
            assertTrue(assertIs<SUCCESS>(success5.state).isNoMore)

            // loadMore past the limit is a no-op
            viewModel.loadMore()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_selectGenre() = runTest {
        viewModel.finalResultFlow.test {
            skipItems(1) // initial state

            viewModel.search("movie")
            skipItems(2) // loading and success: 25 movies (IDs 1–25, genreIds = [id % 3])

            // Select genre 1 → movies where id%3==1: IDs 1,4,7,10,13,16,19,22,25 = 9
            val genre1 = SearchViewModel.ShowGenre(MovieGenre(1, "a"), MutableStateFlow(true))
            viewModel.selectGenre(genre1)
            assertEquals(9, awaitItem().data.size)

            // Also select genre 2 → union adds id%3==2: IDs 2,5,8,11,14,17,20,23 = 8 more
            val genre2 = SearchViewModel.ShowGenre(MovieGenre(2, "b"), MutableStateFlow(true))
            viewModel.selectGenre(genre2)
            assertEquals(17, awaitItem().data.size)

            // Deselect genre 1 → only genre 2 remains, 8 movies
            genre1.isSelected.value = false
            viewModel.selectGenre(genre1)
            assertEquals(8, awaitItem().data.size)

            // Deselect genre 2 → no filter, all 25 movies visible again
            genre2.isSelected.value = false
            viewModel.selectGenre(genre2)
            assertEquals(MockedRepository.TOTAL_RESULTS, awaitItem().data.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_search_error() = runTest {
        val errorViewModel = searchViewModel(ErrorMockedRepository())
        errorViewModel.finalResultFlow.test {
            val initial = awaitItem()
            assertEquals(emptyList(), initial.data)
            assertTrue(initial.state is SUCCESS)

            errorViewModel.search("movie")
            advanceTimeBy(299.toDuration(DurationUnit.MILLISECONDS))
            val loading = awaitItem()
            assertEquals(emptyList(), loading.data)
            assertTrue(loading.state is LOADING)

            val error = awaitItem()
            assertEquals(emptyList(), error.data)
            assertEquals(ErrorMockedRepository.ERROR_MESSAGE, assertIs<ERROR>(error.state).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_prepareGenreList_error() = runTest {
        val errorViewModel = searchViewModel(ErrorMockedRepository())
        errorViewModel.prepareGenreList()?.join()
        assertTrue(errorViewModel.showGenreList.value.isEmpty())
    }
}
