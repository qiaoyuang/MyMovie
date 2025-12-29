package com.qiaoyuang.movie.search

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieGenre
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.jvm.JvmInline
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SearchViewModel(
    private val repository: MovieRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private companion object {
        const val FLOW_SEARCH_WORD = "flow_search_world"
        const val FLOW_PAGE_STATE = "flow_page_state"
        const val RESTORED_SAVED_STATE = "restored_saved_state"
        const val RESTORED_SELECTED_GENRES = "restored_selected_genres"
    }

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

    val searchWordFlow: StateFlow<String>
        field = savedStateHandle.getMutableStateFlow(
            key = FLOW_SEARCH_WORD,
            initialValue = "",
        )

    private val pageStateFlow = savedStateHandle.getMutableStateFlow(
        key = FLOW_PAGE_STATE,
        initialValue = 1,
    )

    private val selectedGenres = HashSet<Int>()
    private val genreFilterFlow = MutableSharedFlow<Set<Int>>(replay = 1)

    val finalResultFlow: StateFlow<DataWithState>
        field = MutableStateFlow<DataWithState>(default)

    private val pageLimit = atomic(Int.MAX_VALUE)

    @OptIn(FlowPreview::class)
    private val combinedFlow = searchWordFlow
        .debounce(300.toDuration(DurationUnit.MILLISECONDS))
        .combine(pageStateFlow) { word, page ->
            if (word.isBlank())
                defaultTriple
            else try {
                val (_, results, totalPages, _) = repository.search(word, page)
                pageLimit.value = totalPages
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

    init {
        restoreSelectedGenres()
        savedStateHandle.setSavedStateProvider(RESTORED_SAVED_STATE) {
            savedState {
                putIntList(RESTORED_SELECTED_GENRES, selectedGenres.toList())
            }
        }
    }

    private fun restoreSelectedGenres() {
        savedStateHandle
            .get<SavedState>(RESTORED_SELECTED_GENRES)
            ?.read {
                getIntList(RESTORED_SELECTED_GENRES)
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        selectedGenres.addAll(it)
                    }
            }
    }

    suspend fun init() {
        genreFilterFlow.emit(selectedGenres)
        combinedFlow.collect { finalResultFlow.value = it }
    }

    fun search(word: String) {
        finalResultFlow.value = emptyLoading
        searchWordFlow.value = word
        pageStateFlow.value = 1
    }

    fun loadMore() {
        if (pageStateFlow.value >= pageLimit.value)
            return
        val (results, _) = finalResultFlow.value
        finalResultFlow.value = DataWithState(results, SearchResultState.LOADING)
        pageStateFlow.value++
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