package com.qiaoyuang.movie.test

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.search.SearchViewModel
import com.qiaoyuang.movie.search.matchesGenres
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchViewModelTest : BasicTest() {

    private fun searchViewModel(repository: MovieRepository = MockedRepository()) =
        SearchViewModel(repository, SavedStateHandle())

    private fun movie(genreIds: List<Int>?) = Movie(
        id = 1L,
        title = "t",
        overview = "o",
        posterPath = null,
        backdropPath = null,
        voteAverage = null,
        genreIds = genreIds,
    )

    // ---- The PagingData pipeline: debounce -> flatMapLatest -> cachedIn -> combine(filter) ----
    //
    // These run on BasicTest as-is: runTest reuses the scheduler of the TestDispatcher that
    // BasicTest installs as Main, so viewModelScope, cachedIn's sharing coroutine and the
    // search debounce all advance on the same virtual clock.
    //
    // Sizes are asserted as lower bounds or properties rather than exact counts, because how
    // many pages Paging prefetches while presenting is its own business, not this pipeline's.

    /** Records which pages were requested, so tests can tell a re-filter from a reload. */
    private class RecordingRepository(
        private val delegate: MovieRepository = MockedRepository(),
    ) : MovieRepository by delegate {
        val searchedPages = mutableListOf<Int>()

        override suspend fun search(word: String, page: Int): Result<MovieResponse, String> {
            searchedPages += page
            return delegate.search(word, page)
        }
    }

    /**
     * Regression test for PagingData.empty(): its no-arg form carries null load states, which
     * the presenter does not dispatch, so asSnapshot (and LazyPagingItems) never saw loading end.
     */
    @Test
    fun test_blank_word_yields_an_empty_list() = runTest {
        assertTrue(searchViewModel().movies.asSnapshot().isEmpty())
    }

    @Test
    fun test_search_loads_results() = runTest {
        val viewModel = searchViewModel()
        viewModel.search("movie")
        assertTrue(viewModel.movies.asSnapshot().size >= MockedRepository.TOTAL_RESULTS)
    }

    @Test
    fun test_genre_filter_keeps_only_matching_movies() = runTest {
        val viewModel = searchViewModel()
        viewModel.search("movie")
        viewModel.toggleGenre(1)
        val filtered = viewModel.movies.asSnapshot()
        // MockedRepository tags movie n with the single genre n % 3.
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.genreIds == listOf(1) })
    }

    @Test
    fun test_toggling_a_genre_off_restores_the_full_list() = runTest {
        val viewModel = searchViewModel()
        viewModel.search("movie")
        viewModel.toggleGenre(1)
        viewModel.toggleGenre(1)
        assertTrue(viewModel.movies.asSnapshot().any { it.genreIds != listOf(1) })
    }

    /**
     * Pins the reason cachedIn sits before the genre combine: a filter change must re-filter
     * the pages already in memory. With cachedIn after combine, a filter change wraps the same
     * raw Pager PagingData in a new filter and hands it to a fresh cache, which collects its
     * page event flow a second time — Paging 3.5.1 rejects that outright with
     * "Attempt to collect twice from pageEventFlow … Did you forget to call cachedIn".
     */
    @Test
    fun test_changing_the_genre_filter_does_not_reload_from_page_one() = runTest {
        val repository = RecordingRepository()
        val viewModel = searchViewModel(repository)
        viewModel.search("movie")
        viewModel.movies.asSnapshot()
        viewModel.toggleGenre(1)
        viewModel.movies.asSnapshot()
        assertEquals(1, repository.searchedPages.count { it == 1 })
    }

    // ---- The genre predicate itself ----

    @Test
    fun test_empty_selection_matches_everything() {
        assertTrue(movie(listOf(7)).matchesGenres(emptySet()))
        assertTrue(movie(null).matchesGenres(emptySet()))
    }

    @Test
    fun test_any_overlap_matches() {
        assertTrue(movie(listOf(1, 5)).matchesGenres(setOf(5, 9)))
        assertFalse(movie(listOf(1, 5)).matchesGenres(setOf(2, 9)))
    }

    @Test
    fun test_movie_without_genres_never_survives_a_selection() {
        assertFalse(movie(null).matchesGenres(setOf(1)))
        assertFalse(movie(emptyList()).matchesGenres(setOf(1)))
    }

    @Test
    fun test_toggleGenre_flips_the_selection() = runTest {
        val viewModel = searchViewModel()
        viewModel.genreFilterState.test {
            assertEquals(emptySet(), awaitItem().selectedIds)
            viewModel.toggleGenre(2)
            assertEquals(setOf(2), awaitItem().selectedIds)
            viewModel.toggleGenre(3)
            assertEquals(setOf(2, 3), awaitItem().selectedIds)
            viewModel.toggleGenre(2)
            assertEquals(setOf(3), awaitItem().selectedIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_prepareGenreList() = runTest {
        val viewModel = searchViewModel()
        viewModel.genreFilterState.test {
            assertTrue(awaitItem().genres.isEmpty())
            viewModel.prepareGenreList()?.join()
            assertEquals(MockedRepository.GENRE_SIZE, awaitItem().genres.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_prepareGenreList_error() = runTest {
        val viewModel = searchViewModel(ErrorMockedRepository())
        viewModel.genreFilterState.test {
            assertTrue(awaitItem().genres.isEmpty())
            viewModel.prepareGenreList()?.join()
            // A failed fetch leaves the catalogue empty, so no new state is emitted
            expectNoEvents()
        }
    }
}
