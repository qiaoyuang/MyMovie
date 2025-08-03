package com.qiaoyuang.movie.search

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieGenre
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.jvm.JvmInline
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SearchViewModel(private val repository: MovieRepository) : ViewModel() {

    sealed interface SearchResultState {

        data object LOADING : SearchResultState

        @JvmInline
        value class SUCCESS(val isNoMore: Boolean = false) : SearchResultState

        data object ERROR : SearchResultState
    }

    typealias DataWithState = Pair<List<ApiMovie>, SearchResultState>

    private val emptyList = emptyList<ApiMovie>()
    private val default = DataWithState(emptyList, SearchResultState.SUCCESS())
    private val emptyLoading = DataWithState(emptyList, SearchResultState.LOADING)
    private val defaultTriple = Triple(1, emptyList, SearchResultState.SUCCESS())

    private val _searchWordFlow = MutableStateFlow("")
    val searchWordFlow: StateFlow<String> = _searchWordFlow

    private val _pageStateFlow = MutableStateFlow(1)

    private val selectedGenres = HashSet<Int>()
    private val genreFilterFlow = MutableSharedFlow<Set<Int>>(replay = 1)

    private val _finalResultFlow = MutableStateFlow<DataWithState>(default)
    val finalResultFlow: StateFlow<DataWithState> = _finalResultFlow

    @OptIn(FlowPreview::class)
    private val combinedFlow = _searchWordFlow
        .debounce(300.toDuration(DurationUnit.MILLISECONDS))
        .combine(_pageStateFlow) { word, page ->
            if (word.isBlank())
                defaultTriple
            else try {
                val (_, results, totalPages, _) = repository.search(word, page)
                Triple(page, results, SearchResultState.SUCCESS(page == totalPages))
            } catch (e: Exception) {
                e.printStackTrace()
                Triple(page, emptyList, SearchResultState.ERROR)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), defaultTriple)
        .combine(genreFilterFlow) { triple, set -> triple to set }
        .scan(default) { (oldResults, _), (dataWithState, set) ->
            val (page, newResults, state) = dataWithState
            val filterResults = newResults.filter { movie ->
                set.isEmpty() || movie.genreIds?.any { id -> set.contains(id) } == true
            }
            val results = if (page == 1) filterResults else oldResults + filterResults
            DataWithState(results, state)
        }
        .flowOn(Dispatchers.Default)

    suspend fun init() {
        genreFilterFlow.emit(selectedGenres)
        combinedFlow.collect { _finalResultFlow.value = it }
    }

    fun search(word: String) {
        _finalResultFlow.value = emptyLoading
        _searchWordFlow.value = word
        _pageStateFlow.value = 1
    }

    fun loadMore() {
        val (results, _) = finalResultFlow.value
        _finalResultFlow.value = DataWithState(results, SearchResultState.LOADING)
        _pageStateFlow.value++
    }

    data class ShowGenre(
        val genre: MovieGenre,
        val isSelected: MutableState<Boolean> = mutableStateOf(false),
    )

    val showGenreList = mutableStateOf(emptyList<ShowGenre>())

    fun prepareGenreList(): Job? {
        if (showGenreList.value.isNotEmpty())
            return null
        return viewModelScope.launch(Dispatchers.Default) {
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
}