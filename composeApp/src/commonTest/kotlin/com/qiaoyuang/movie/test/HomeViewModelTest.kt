package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.home.HomeViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @BeforeTest
    @OptIn(DelicateCoroutinesApi::class)
    fun setUp() {
        val mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @Test
    fun testGetMovies() = runTest {
        val viewModel = HomeViewModel(MockedRepository())
        viewModel.getTopMovies(false).join()
        assertEquals(5, (viewModel.movieState.value as? HomeViewModel.TopMoviesState.SHOW)?.value?.size ?: 0)
        repeat(4) {
            viewModel.getTopMovies(true).join()
        }
        assertEquals(25, (viewModel.movieState.value as? HomeViewModel.TopMoviesState.SHOW)?.value?.size ?: 0)
        viewModel.getTopMovies(true).join()
        assertEquals(25, (viewModel.movieState.value as? HomeViewModel.TopMoviesState.SHOW)?.value?.size ?: 0)
    }

    @AfterTest
    fun testDown() {
        Dispatchers.resetMain()
    }
}