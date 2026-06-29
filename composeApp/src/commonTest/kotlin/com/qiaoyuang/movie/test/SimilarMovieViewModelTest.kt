package com.qiaoyuang.movie.test

import app.cash.turbine.turbineScope
import com.qiaoyuang.movie.model.ui.UIEvent
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimilarMovieViewModelTest : BasicTest() {

    @Test
    fun test_getSimilarMovies() = runTest {
        turbineScope {
            val viewModel = SimilarMoviesViewModel(MockedRepository(), 1L)
            val events = viewModel.uiEventFlow.testIn(backgroundScope)
            val states = viewModel.movieState.testIn(backgroundScope)

            val state1 = states.awaitItem()
            assertEquals(emptyList(), state1.data)
            assertFalse(state1.isNoMore)
            assertFalse(state1.isLoading)
            assertFalse(state1.isError)

            viewModel.getSimilarMovies()
            val state2 = states.awaitItem()
            assertEquals(emptyList(), state2.data)
            assertTrue(state2.isLoading)

            val state3 = states.awaitItem()
            assertEquals(MockedRepository.COUNT, state3.data.size)
            assertFalse(state3.isNoMore)
            assertFalse(state3.isLoading)

            repeat(4) {
                viewModel.getSimilarMovies()
            }
            // 4 calls × (LOADING + SUCCESS) − 1, leaving the final SUCCESS to await below
            states.skipItems(2 * 4 - 1)
            val total = MockedRepository.COUNT * MockedRepository.TOTAL_PAGES
            val state4 = states.awaitItem()
            assertEquals(total, state4.data.size)
            assertFalse(state4.isNoMore)

            // One more page past the limit: no new data, isNoMore flips, and a toast fires
            viewModel.getSimilarMovies()
            val state5 = states.awaitItem()
            assertEquals(total, state5.data.size)
            assertTrue(state5.isNoMore)
            assertEquals(UIEvent.CommonNoMoreToast, events.awaitItem())

            states.cancelAndIgnoreRemainingEvents()
            events.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_getSimilarMovies_error() = runTest {
        turbineScope {
            val viewModel = SimilarMoviesViewModel(ErrorMockedRepository(), 1L)
            val events = viewModel.uiEventFlow.testIn(backgroundScope)
            val states = viewModel.movieState.testIn(backgroundScope)

            val initial = states.awaitItem()
            assertEquals(emptyList(), initial.data)
            assertFalse(initial.isError)

            viewModel.getSimilarMovies()
            val loading = states.awaitItem()
            assertTrue(loading.isLoading)
            assertFalse(loading.isError)

            val error = states.awaitItem()
            assertEquals(emptyList(), error.data)
            assertFalse(error.isLoading)
            assertTrue(error.isError)
            assertEquals(UIEvent.CommonErrorToast, events.awaitItem())

            states.cancelAndIgnoreRemainingEvents()
            events.cancelAndIgnoreRemainingEvents()
        }
    }
}
