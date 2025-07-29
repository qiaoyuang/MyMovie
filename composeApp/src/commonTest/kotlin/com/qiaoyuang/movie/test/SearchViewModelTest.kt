package com.qiaoyuang.movie.test

import androidx.compose.runtime.mutableStateOf
import com.qiaoyuang.movie.model.MovieGenre
import com.qiaoyuang.movie.search.SearchViewModel
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.SUCCESS
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.EMPTY
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchViewModelTest : BasicTest() {

    private val viewModel = SearchViewModel(MockedRepository())

    override fun setUp() {
        super.setUp()
        runTest {
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
        val channel = viewModel.finalResultFlow.produceIn(backgroundScope)
        assertEquals(EMPTY, channel.receive())
        viewModel.searchWordFlow.value = "ABC"
        assertEquals(MockedRepository.TOTAL_RESULTS, (channel.receive() as? SUCCESS)?.value?.size)
        viewModel.selectGenre(SearchViewModel.ShowGenre(
            genre = MovieGenre(1, "a"),
            isSelected = mutableStateOf(true),
        ))
        val count1 = MockedRepository.TOTAL_RESULTS / 3 + if (MockedRepository.TOTAL_RESULTS % 3 > 1) 1 else 0
        assertEquals(count1, (channel.receive() as? SUCCESS)?.value?.size)
        viewModel.selectGenre(SearchViewModel.ShowGenre(
            genre = MovieGenre(2, "b"),
            isSelected = mutableStateOf(true),
        ))
        val count2 = count1 + MockedRepository.TOTAL_RESULTS / 3 + if (MockedRepository.TOTAL_RESULTS % 3 > 2) 1 else 0
        assertEquals(count2, (channel.receive() as? SUCCESS)?.value?.size)
        viewModel.selectGenre(SearchViewModel.ShowGenre(
            genre = MovieGenre(1, "a"),
            isSelected = mutableStateOf(false),
        ))
        assertEquals(count1, (channel.receive() as? SUCCESS)?.value?.size)
    }
}