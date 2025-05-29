package com.qiaoyuang.movie.search

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieGenre
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SearchViewModel(private val repository: MovieRepository) : ViewModel() {

    var searchWord by mutableStateOf("")

    private val requestResultFlow = MutableSharedFlow<String>()

    private val selectedGenres = HashSet<Int>()
    private val genreFilterFlow = MutableSharedFlow<Set<Int>>(replay = 1)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            genreFilterFlow.emit(selectedGenres)
        }
    }

    @OptIn(FlowPreview::class)
    val finalResultFlow = requestResultFlow
        .debounce(300.toDuration(DurationUnit.MILLISECONDS))
        .combine(genreFilterFlow) { word, set ->
            if (word.isEmpty()) {
                SearchResultState.EMPTY
            } else try {
                SearchResultState.SUCCESS(
                    value = repository.search(word).results.filter { movie ->
                        set.isEmpty() || movie.genreIds?.any { id -> set.contains(id) } == true
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                SearchResultState.ERROR
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchResultState.EMPTY)

    data class ShowGenre(
        val genre: MovieGenre,
        val isSelected: MutableState<Boolean> = mutableStateOf(false),
    )

    val showGenreList = mutableStateOf(emptyList<ShowGenre>())

    fun prepareGenreList() {
        if (showGenreList.value.isNotEmpty())
            return
        viewModelScope.launch(Dispatchers.Default) {
            showGenreList.value = repository.getMovieGenreList().map {
                ShowGenre(it)
            }
        }
    }

    fun selectGenre(genre: ShowGenre) = viewModelScope.launch(Dispatchers.Default) {
        if (genre.isSelected.value) {
            selectedGenres.add(genre.genre.id)
        } else {
            selectedGenres.remove(genre.genre.id)
        }
        genreFilterFlow.emit(selectedGenres)
    }

    fun query(word: String) = viewModelScope.launch(Dispatchers.Default) {
        requestResultFlow.emit(word)
    }

    sealed interface SearchResultState {

        data object EMPTY : SearchResultState
        data class SUCCESS(val value: List<ApiMovie>) : SearchResultState
        data object ERROR : SearchResultState
    }
}