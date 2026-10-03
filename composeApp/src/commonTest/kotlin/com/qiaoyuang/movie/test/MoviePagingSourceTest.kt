package com.qiaoyuang.movie.test

import androidx.paging.PagingSource
import com.qiaoyuang.movie.model.MovieDataException
import com.qiaoyuang.movie.model.MoviePagingSource
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Replaces HomeViewModelTest and SimilarMovieViewModelTest. With the pagination state machine
 * inside Paging, the thing worth asserting is how an endpoint maps onto LoadResult — which
 * needs no ViewModel, no Main dispatcher and no Turbine.
 */
class MoviePagingSourceTest {

    private fun topRated(repository: MovieRepository = MockedRepository()) =
        MoviePagingSource { page -> repository.fetchTopRated(page) }

    private fun similar(repository: MovieRepository = MockedRepository()) =
        MoviePagingSource { page -> repository.similarMovies(MOVIE_ID, page) }

    private fun refresh(key: Int? = null) = PagingSource.LoadParams.Refresh(
        key = key,
        loadSize = MockedRepository.COUNT,
        placeholdersEnabled = false,
    )

    private fun append(key: Int) = PagingSource.LoadParams.Append(
        key = key,
        loadSize = MockedRepository.COUNT,
        placeholdersEnabled = false,
    )

    private suspend fun MoviePagingSource.loadPage(params: PagingSource.LoadParams<Int>) =
        assertIs<PagingSource.LoadResult.Page<Int, Movie>>(load(params))

    @Test
    fun test_first_page() = runTest {
        val page = topRated().loadPage(refresh())
        assertEquals(MockedRepository.COUNT, page.data.size)
        // prevKey stays null so Paging never asks for a Prepend.
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun test_middle_page_keeps_appending() = runTest {
        assertEquals(3, topRated().loadPage(append(2)).nextKey)
    }

    @Test
    fun test_last_page_ends_pagination() = runTest {
        // A null nextKey is how endOfPaginationReached becomes true in the UI.
        assertNull(topRated().loadPage(append(MockedRepository.TOTAL_PAGES)).nextKey)
    }

    @Test
    fun test_error_is_reported_as_load_error() = runTest {
        val result = topRated(ErrorMockedRepository()).load(refresh())
        val error = assertIs<PagingSource.LoadResult.Error<Int, Movie>>(result)
        // Passed through as-is: no MovieLoadException wrapper hiding what kind of failure it was.
        assertSame(ErrorMockedRepository.ERROR, error.throwable)
    }

    @Test
    fun test_similar_movies_endpoint() = runTest {
        val page = similar().loadPage(refresh())
        assertEquals(MockedRepository.COUNT, page.data.size)
        assertEquals(2, page.nextKey)
        assertNull(similar().loadPage(append(MockedRepository.TOTAL_PAGES)).nextKey)
    }

    /**
     * Regression test: nextKey used to be derived from the page the response echoes back.
     * MockedRepository.search reported page 1 for every request, so every page produced
     * nextKey = 2 and Paging aborted with "the same value was passed as the nextKey in two
     * sequential Pages". Deriving it from the requested key keeps the keys monotonic.
     */
    @Test
    fun test_next_key_follows_the_requested_page() = runTest {
        val source = MoviePagingSource { page ->
            // A server that always claims to be on page 1, whatever was asked for.
            MockedRepository().fetchTopRated(page).let { result ->
                assertIs<com.qiaoyuang.movie.model.Result.Success<MovieResponse>>(result)
                com.qiaoyuang.movie.model.Result.Success(result.data.copy(page = 1))
            }
        }
        assertEquals(3, source.loadPage(append(2)).nextKey)
        assertEquals(5, source.loadPage(append(4)).nextKey)
    }

    @Test
    fun test_similar_movies_error() = runTest {
        val result = similar(ErrorMockedRepository()).load(refresh())
        val error = assertIs<PagingSource.LoadResult.Error<Int, Movie>>(result)
        // Passed through as-is: no MovieLoadException wrapper hiding what kind of failure it was.
        assertSame(ErrorMockedRepository.ERROR, error.throwable)
    }

    private companion object {
        const val MOVIE_ID = 1L
    }

    /**
     * Pages that overlap, as TMDB's do when the result set shifts between requests: page n ends
     * with the movie page n+1 begins with.
     */
    private class OverlappingRepository(
        private val pageSize: Int = 3,
        private val totalPages: Int = 3,
    ) : MovieRepository by MockedRepository() {
        override suspend fun search(word: String, page: Int): Result<MovieResponse, MovieDataException> {
            val first = (page - 1L) * pageSize - (page - 1L) + 1
            return Result.Success(
                MovieResponse(
                    page = page,
                    results = (first until first + pageSize).map { id ->
                        Movie(
                            id = id,
                            title = "title$id",
                            overview = "overview$id",
                            posterPath = null,
                            backdropPath = null,
                            voteAverage = 7.0,
                            genreIds = null,
                        )
                    },
                    totalPages = totalPages,
                )
            )
        }
    }

    private fun search(repository: MovieRepository) =
        MoviePagingSource { page -> repository.search("word", page) }

    /**
     * The crash this guards against: two items with the same id make LazyColumn's key collide
     * and throw "Key ... was already used".
     */
    @Test
    fun test_a_movie_repeated_from_an_earlier_page_is_dropped() = runTest {
        val source = search(OverlappingRepository())

        val first = source.loadPage(refresh())
        val second = source.loadPage(append(2))

        assertEquals(listOf(1L, 2L, 3L), first.data.map(Movie::id))
        // Page two repeats movie 3, which page one already produced.
        assertEquals(listOf(4L, 5L), second.data.map(Movie::id))
        val all = (first.data + second.data).map(Movie::id)
        assertEquals(all.distinct(), all)
    }

    @Test
    fun test_a_movie_repeated_inside_one_page_is_dropped() = runTest {
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun search(word: String, page: Int): Result<MovieResponse, MovieDataException> =
                Result.Success(
                    MovieResponse(
                        page = page,
                        results = listOf(1L, 2L, 1L).map { id ->
                            Movie(id, "t$id", "o$id", null, null, 7.0, null)
                        },
                        totalPages = 1,
                    )
                )
        }

        assertEquals(listOf(1L, 2L), search(repo).loadPage(refresh()).data.map(Movie::id))
    }

    /** A page emptied by de-duplication must not look like the end of the list. */
    @Test
    fun test_an_entirely_duplicate_page_still_offers_the_next_key() = runTest {
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun search(word: String, page: Int): Result<MovieResponse, MovieDataException> =
                Result.Success(
                    MovieResponse(
                        page = page,
                        results = listOf(Movie(1L, "t", "o", null, null, 7.0, null)),
                        totalPages = 5,
                    )
                )
        }
        val source = search(repo)

        source.loadPage(refresh())
        val second = source.loadPage(append(2))

        assertEquals(emptyList(), second.data)
        assertEquals(3, second.nextKey)
    }

    /**
     * De-duplication is per generation. Paging builds a new source on refresh, so the movies it
     * already produced have to come back — otherwise a refresh would return an empty list.
     */
    @Test
    fun test_a_new_generation_starts_de_duplicating_from_scratch() = runTest {
        val repository = OverlappingRepository()
        val firstGeneration = search(repository)
        firstGeneration.loadPage(refresh())

        val afterRefresh = search(repository).loadPage(refresh())

        assertEquals(listOf(1L, 2L, 3L), afterRefresh.data.map(Movie::id))
    }
}
