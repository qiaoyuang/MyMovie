package com.qiaoyuang.movie.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SearchViewModel(private val repository: MovieRepository) : ViewModel() {

    private val searchWordChannel = Channel<String>()

    var searchWord by mutableStateOf("")

    @OptIn(FlowPreview::class)
    val searchResultFlow: Flow<SearchResultState> = flow {
        for (word in searchWordChannel)
            emit(word)
    }.debounce(300.toDuration(DurationUnit.MILLISECONDS))
        .map {
            if (it.isEmpty()) {
                SearchResultState.EMPTY
            } else try {
                SearchResultState.SUCCESS(repository.search(it).results)
            } catch (e: Exception) {
                e.printStackTrace()
                SearchResultState.ERROR
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchResultState.EMPTY)

    fun query(word: String) = viewModelScope.launch(Dispatchers.Default) {
        searchWordChannel.send(word)
    }

    sealed interface SearchResultState {

        data object EMPTY : SearchResultState

        data class SUCCESS(val value: List<ApiMovie>) : SearchResultState

        data object ERROR : SearchResultState
    }
}