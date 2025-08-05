package com.qiaoyuang.movie.test

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieGenre
import com.qiaoyuang.movie.search.SearchViewModel
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.SUCCESS
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.LOADING
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchViewModelTest : BasicTest() {

    private val viewModel = SearchViewModel(MockedRepository())

    override fun setUp() {
        super.setUp()
        viewModel.viewModelScope.launch {
            viewModel.init()
        }
    }

    @Test
    fun test_prepareGenreList() = runTest {
        viewModel.prepareGenreList()?.join()
        assertEquals(MockedRepository.GENRE_SIZE, viewModel.showGenreList.value.size)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun test_search() = runTest {
        val emptyList = emptyList<ApiMovie>()

        val channel = viewModel.finalResultFlow.produceIn(backgroundScope)
        assertEquals(emptyList to SUCCESS(), channel.receive())

        viewModel.search("ABC")
        val (data0, state0) = channel.receive()
        assertEquals(emptyList, data0)
        assertEquals(LOADING, state0)

        val (data1, state1) = channel.receive()
        assertEquals(MockedRepository.TOTAL_RESULTS, data1.size)
        assertTrue(state1 is SUCCESS)

        println(data1.size)

        viewModel.selectGenre(SearchViewModel.ShowGenre(
            genre = MovieGenre(1, "a"),
            isSelected = mutableStateOf(true),
        ))
        val (data2, state2) = channel.receive()
        val count1 = MockedRepository.TOTAL_RESULTS / 3 + if (MockedRepository.TOTAL_RESULTS % 3 >= 1) 1 else 0
        assertEquals(count1, data2.size)
        assertTrue(state2 is SUCCESS)

        println(data2.size)

        viewModel.selectGenre(SearchViewModel.ShowGenre(
            genre = MovieGenre(2, "b"),
            isSelected = mutableStateOf(true),
        ))
        val (data3, state3) = channel.receive()
        val count2 = count1 + MockedRepository.TOTAL_RESULTS / 3 + if (MockedRepository.TOTAL_RESULTS % 3 >= 2) 1 else 0
        assertEquals(count2, data3.size)
        assertTrue(state3 is SUCCESS)

        println(data3.size)

        viewModel.selectGenre(SearchViewModel.ShowGenre(
            genre = MovieGenre(2, "b"),
            isSelected = mutableStateOf(false),
        ))
        val (data4, state4) = channel.receive()
        println(data4.size)
        assertEquals(count1, data4.size)
        assertTrue(state4 is SUCCESS)
    }
}