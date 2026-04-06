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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.jvm.JvmInline
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SearchViewModel(
    private val repository: MovieRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private companion object {
        const val FLOW_SEARCH_WORD = "flow_search_word"
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

    private data class ScanState(
        val data: DataWithState,
        val prevSet: Set<Int> = emptySet(),
        val prevPageDataState: Triple<Int, List<ApiMovie>, SearchResultState>? = null,
    )

    private val emptyList = emptyList<ApiMovie>()
    private val default = DataWithState(emptyList, SearchResultState.SUCCESS())
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
    private val genreFilterFlow = MutableStateFlow(selectedGenres.toSet())

    private val pageLimit = atomic(Int.MAX_VALUE)

    private val fullData = ArrayList<ApiMovie>()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val finalResultFlow = searchWordFlow
        .debounce(300.toDuration(DurationUnit.MILLISECONDS))
        .combine(pageStateFlow) { word, page -> word to page }
        .flatMapLatest { (word, page) ->
            flow {
                emit(Triple(page, emptyList, SearchResultState.LOADING))
                if (word.isBlank()) {
                    emit(defaultTriple)
                    return@flow
                }
                else try {
                    val (_, results, totalPages, _) = repository.search(word, page)
                    pageLimit.value = totalPages
                    emit(Triple(page, results, SearchResultState.SUCCESS(page == totalPages)))
                } catch (e: Exception) {
                    e.printStackTrace()
                    emit(Triple(page, emptyList, SearchResultState.ERROR))
                }
            }
        }
        .combine(genreFilterFlow.debounce(100.toDuration(DurationUnit.MILLISECONDS))) { pageDataState, set -> pageDataState to set }
        .scan(ScanState(default)) { acc, (pageDataState, set) ->
            val (page, newResults, state) = pageDataState
            val pageDataChanged = pageDataState !== acc.prevPageDataState
            if (pageDataChanged) {
                if (page == 1)
                    fullData.clear()
                fullData.addAll(newResults)
            }
            val results = if (set == acc.prevSet) {
                if (!pageDataChanged) {
                    acc.data.first
                } else {
                    val filterResults = if (set.isEmpty())
                        newResults
                    else newResults.filter { movie ->
                        movie.genreIds?.any { id -> set.contains(id) } == true
                    }
                    if (page == 1) filterResults else acc.data.first + filterResults
                }
            } else {
                if (set.isEmpty())
                    fullData.toList()
                else
                    fullData.filter { movie ->
                        movie.genreIds?.any { id -> set.contains(id) } == true
                    }
            }
            ScanState(DataWithState(results, state), set, pageDataState)
        }
        .map { it.data }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000.toDuration(DurationUnit.MILLISECONDS)), default)


    init {
        restoreSelectedGenres()
        savedStateHandle.setSavedStateProvider(RESTORED_SAVED_STATE) {
            savedState {
                putIntList(RESTORED_SELECTED_GENRES, genreFilterFlow.value.toList())
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
                        genreFilterFlow.value = selectedGenres.toSet()
                    }
            }
    }

    fun search(word: String) {
        searchWordFlow.value = word
        pageStateFlow.value = 1
    }

    fun loadMore() {
        if (pageStateFlow.value >= pageLimit.value)
            return
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

    private val genreMutex = Mutex()

    fun selectGenre(genre: ShowGenre) = viewModelScope.launch(Dispatchers.Default) {
        genreMutex.withLock {
            if (genre.isSelected.value) {
                selectedGenres.add(genre.genre.id)
            } else {
                selectedGenres.remove(genre.genre.id)
            }
            genreFilterFlow.value = selectedGenres.toSet()
        }
    }
}