package com.qiaoyuang.movie.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import com.qiaoyuang.movie.model.MOVIE_PAGE_SIZE
import com.qiaoyuang.movie.model.MoviePagingSource
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.qiaoyuang.movie.model.MovieDataException

internal class SearchViewModel(
    private val repository: MovieRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private companion object {
        const val FLOW_SEARCH_WORD = "flow_search_word"
        const val RESTORED_SAVED_STATE = "restored_saved_state"
        const val RESTORED_SELECTED_GENRES = "restored_selected_genres"

        val SEARCH_DEBOUNCE = 300.milliseconds

        // The no-arg PagingData.empty() carries null load states, so a presenter receiving it
        // never learns that loading is over: LazyPagingItems can keep showing the previous
        // search's Loading, and paging-testing's asSnapshot waits forever. Stating explicitly
        // that the list is idle and complete in both directions settles it.
        val BLANK_WORD_LOAD_STATES = LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        )
        val SUBSCRIPTION_TIMEOUT = 5.seconds
    }

    val searchWordFlow: StateFlow<String>
        field = savedStateHandle.getMutableStateFlow(
            key = FLOW_SEARCH_WORD,
            initialValue = "",
        )

    // The genre catalogue. Fetched once by prepareGenreList(); its list reference
    // stays the same afterwards, so toggling a filter never rebuilds it.
    private val genresFlow = MutableStateFlow<List<MovieGenre>>(emptyList())

    // Single source of truth for the selected genres. Both the filter below and the
    // checkmarks in the UI are derived from this one set, so they cannot drift apart.
    // Only the ViewModel ever writes to it.
    private val selectedGenreIdsFlow = MutableStateFlow<Set<Int>>(emptySet())

    /**
     * The page cursor used to live in its own SavedStateHandle entry while the loaded pages
     * lived in a scan() accumulator. Process death restored the cursor but not the data, so
     * the list came back showing page five with nothing above it. Paging keeps the cursor
     * inside LoadResult.Page, next to the data it indexes, so they can no longer diverge.
     *
     * cachedIn() sits *before* the genre filter on purpose: toggling a genre then re-filters
     * the pages already in memory, while only a new search word builds a new Pager.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val movies: Flow<PagingData<Movie>> = searchWordFlow
        .debounce(SEARCH_DEBOUNCE)
        .flatMapLatest { word ->
            // A blank box asks for nothing rather than searching for the empty string.
            if (word.isBlank())
                flowOf(PagingData.empty(sourceLoadStates = BLANK_WORD_LOAD_STATES))
            else
                Pager(
                    config = PagingConfig(pageSize = MOVIE_PAGE_SIZE, enablePlaceholders = false),
                    pagingSourceFactory = { MoviePagingSource { page -> repository.search(word, page) } },
                ).flow
        }
        .cachedIn(viewModelScope)
        .combine(selectedGenreIdsFlow) { pagingData, selected ->
            if (selected.isEmpty())
                pagingData
            else
                pagingData.filter {
                    it.matchesGenres(selected)
                }
        }

    init {
        restoreSelectedGenres()
        savedStateHandle.setSavedStateProvider(RESTORED_SAVED_STATE) {
            savedState {
                putIntList(RESTORED_SELECTED_GENRES, selectedGenreIdsFlow.value.toList())
            }
        }
    }

    private fun restoreSelectedGenres() {
        savedStateHandle
            .get<SavedState>(RESTORED_SAVED_STATE)
            ?.read { getIntList(RESTORED_SELECTED_GENRES) }
            ?.let { selectedGenreIdsFlow.value = it.toSet() }
    }

    /** No page to reset: flatMapLatest builds a fresh Pager, which starts at page one. */
    fun search(word: String) {
        searchWordFlow.value = word
    }

    /**
     * Immutable snapshot of the genre filter. The catalogue and the selection are kept as
     * separate fields on purpose: toggling a genre allocates only a new Set, while [genres]
     * keeps the same reference, so the item list is never rebuilt.
     */
    data class GenreFilterState(
        val genres: List<MovieGenre> = emptyList(),
        val selectedIds: Set<Int> = emptySet(),
    )

    val genreFilterState: StateFlow<GenreFilterState> =
        combine(genresFlow, selectedGenreIdsFlow, ::GenreFilterState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
                initialValue = GenreFilterState(),
            )

    fun prepareGenreList(): Job? {
        if (genresFlow.value.isNotEmpty())
            return null
        return viewModelScope.launch {
            genresFlow.value = when (val result = repository.getMovieGenreList()) {
                is Result.Success<List<MovieGenre>> -> result.data
                is Result.Error<MovieDataException> -> emptyList()
            }
        }
    }

    /**
     * The only way the selection changes. The UI reports a click and reads the result back
     * from [genreFilterState]; it no longer mutates any state itself. update() is an atomic
     * compare-and-set loop, so no Mutex is needed.
     */
    fun toggleGenre(genreId: Int) {
        selectedGenreIdsFlow.update { selected ->
            if (genreId in selected) selected - genreId else selected + genreId
        }
    }
}

/**
 * Extracted from the PagingData filter so the rule itself can be unit-tested: PagingData is
 * opaque, and paging-testing's asSnapshot does not run under runTest's virtual clock.
 *
 * A movie with no genreIds at all never survives a non-empty selection.
 */
internal fun Movie.matchesGenres(selected: Set<Int>): Boolean =
    selected.isEmpty() || genreIds?.any { it in selected } == true
