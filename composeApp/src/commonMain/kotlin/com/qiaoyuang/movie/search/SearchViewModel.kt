package com.qiaoyuang.movie.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SearchViewModel(
    private val repository: MovieRepository,
    private val savedStateHandle: SavedStateHandle,
    defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private companion object {
        const val FLOW_SEARCH_WORD = "flow_search_word"
        const val FLOW_PAGE_STATE = "flow_page_state"
        const val RESTORED_SAVED_STATE = "restored_saved_state"
        const val RESTORED_SELECTED_GENRES = "restored_selected_genres"
    }

    sealed interface SearchResultState {

        data object LOADING : SearchResultState

        data class SUCCESS(val isNoMore: Boolean = false) : SearchResultState

        data class ERROR(val message: String) : SearchResultState
    }

    private data class PageDataState(
        val page: Int,
        val data: List<Movie>,
        val searchResultState: SearchResultState,
    )

    private data class ScanState(
        val filteredData: DataWithState,
        val accumulatedFullData: List<Movie> = emptyList(),
        val prevPageDataState: PageDataState? = null,
    )

    data class DataWithState(
        val data: List<Movie>,
        val state: SearchResultState,
    )

    private val emptyList = emptyList<Movie>()
    private val defaultDataWithState = DataWithState(emptyList, SearchResultState.SUCCESS())
    private val defaultPageDataState = PageDataState(1, emptyList, SearchResultState.SUCCESS())

    val searchWordFlow: StateFlow<String>
        field = savedStateHandle.getMutableStateFlow(
            key = FLOW_SEARCH_WORD,
            initialValue = "",
        )

    private val pageStateFlow = savedStateHandle.getMutableStateFlow(
        key = FLOW_PAGE_STATE,
        initialValue = 1,
    )

    // The genre catalogue. Fetched once by prepareGenreList(); its list reference
    // stays the same afterwards, so toggling a filter never rebuilds it.
    private val genresFlow = MutableStateFlow<List<MovieGenre>>(emptyList())

    // Single source of truth for the selected genres. Both the filter pipeline below
    // and the checkmarks in the UI are derived from this one set, so they cannot
    // drift apart. Only the ViewModel ever writes to it.
    private val selectedGenreIdsFlow = MutableStateFlow<Set<Int>>(emptySet())

    private val pageLimit = atomic(Int.MAX_VALUE)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val finalResultFlow = searchWordFlow
        .debounce(300.toDuration(DurationUnit.MILLISECONDS))
        .combine(pageStateFlow) { word, page -> word to page }
        .flatMapLatest { (word, page) ->
            flow {
                emit(PageDataState(page, emptyList, SearchResultState.LOADING))
                if (word.isBlank()) {
                    emit(defaultPageDataState)
                    return@flow
                }
                val pageDataState = when (val result = repository.search(word, page)) {
                    is Result.Success<MovieResponse> -> {
                        val (_, results, totalPages) = result.data
                        pageLimit.value = totalPages
                        PageDataState(page, results, SearchResultState.SUCCESS(page == totalPages))
                    }
                    is Result.Error<String> -> PageDataState(page, emptyList, SearchResultState.ERROR(result.error))
                }
                emit(pageDataState)
            }
        }
        .combine(selectedGenreIdsFlow.debounce(100.toDuration(DurationUnit.MILLISECONDS))) { pageDataState, set -> pageDataState to set }
        .scan(ScanState(defaultDataWithState)) { (_, accumulatedFullData, prevPageDataState), (pageDataState, set) ->
            val (page, newResults, state) = pageDataState
            val pageDataChanged = pageDataState !== prevPageDataState

            val nextFullData = if (pageDataChanged) {
                if (page == 1) newResults else accumulatedFullData + newResults
            } else {
                accumulatedFullData
            }

            val nextFilteredList = if (set.isEmpty()) {
                nextFullData
            } else {
                nextFullData.filter { movie ->
                    movie.genreIds?.any { id -> set.contains(id) } == true
                }
            }

            ScanState(
                filteredData = DataWithState(nextFilteredList, state),
                accumulatedFullData = nextFullData,
                prevPageDataState = pageDataState
            )
        }
        .map { it.filteredData }
        .flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000.toDuration(DurationUnit.MILLISECONDS)), defaultDataWithState)


    init {
        restoreSelectedGenres()
        savedStateHandle.setSavedStateProvider(RESTORED_SAVED_STATE) {
            savedState {
                putIntList(RESTORED_SELECTED_GENRES, selectedGenreIdsFlow.value.toList())
            }
        }
    }

    private fun restoreSelectedGenres() {
        // Bug fix: this used to read key RESTORED_SELECTED_GENRES, while the provider
        // above writes the SavedState under RESTORED_SAVED_STATE, so the lookup always
        // returned null and the selection was never actually restored.
        savedStateHandle
            .get<SavedState>(RESTORED_SAVED_STATE)
            ?.read { getIntList(RESTORED_SELECTED_GENRES) }
            ?.let { selectedGenreIdsFlow.value = it.toSet() }
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

    /**
     * Immutable snapshot of the genre filter. The catalogue and the selection are kept
     * as separate fields on purpose: toggling a genre allocates only a new Set, while
     * [genres] keeps the same reference, so the item list is never rebuilt.
     *
     * Previously each item carried its own MutableStateFlow<Boolean>, which the UI wrote
     * to directly. That was a second copy of the selection state, and it was the one the
     * checkmarks read from — so after process death the restored filter applied to the
     * results while every checkbox rendered as unchecked. There is now only one copy.
     */
    data class GenreFilterState(
        val genres: List<MovieGenre> = emptyList(),
        val selectedIds: Set<Int> = emptySet(),
    )

    val genreFilterState: StateFlow<GenreFilterState> =
        combine(genresFlow, selectedGenreIdsFlow, ::GenreFilterState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000.toDuration(DurationUnit.MILLISECONDS)),
                initialValue = GenreFilterState(),
            )

    fun prepareGenreList(): Job? {
        if (genresFlow.value.isNotEmpty())
            return null
        return viewModelScope.launch {
            genresFlow.value = when (val result = repository.getMovieGenreList()) {
                is Result.Success<List<MovieGenre>> -> result.data
                is Result.Error<String> -> emptyList()
            }
        }
    }

    /**
     * The only way the selection changes. The UI reports a click and reads the result
     * back from [genreFilterState]; it no longer mutates any state itself.
     *
     * update() is an atomic compare-and-set loop, so the read-modify-write is safe
     * without the Mutex this used to need (that Mutex could not help anyway, because
     * the "read" half used to happen in the UI).
     */
    fun toggleGenre(genreId: Int) = selectedGenreIdsFlow.update { selected ->
        if (genreId in selected) selected - genreId else selected + genreId
    }
}