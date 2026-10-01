package com.qiaoyuang.movie.test

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.domain.SimilarMovieUseCaseImpl
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.domain.SimilarMovieShowModel
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import com.qiaoyuang.movie.model.MovieDataException

class SimilarMovieUseCaseImplTest : BasicTest() {

    @Test
    fun test_returns_success_with_movie_list() = runTest {
        val useCase = SimilarMovieUseCaseImpl(MockedRepository(), mainThreadSurrogate, 1L)
        val result = useCase()
        assertIs<Result.Success<List<SimilarMovieShowModel>?>>(result)
        assertEquals(MockedRepository.COUNT, result.data?.size)
    }

    /**
     * The use case used to memoise its result; the repository is cache-first now, so it does
     * not. What has to hold instead is that it goes through that cache-first entry point:
     * calling similarMovies() here would compile, look identical, and silently put the detail
     * strip back on the network. "No second request" is a repository property now, covered by
     * MovieRepositoryImplTest.
     */
    @Test
    fun test_reads_through_the_cache_first_entry_point() = runTest {
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun similarMovies(movieId: Long, page: Int): Result<MovieResponse, MovieDataException> =
                throw AssertionError("the detail strip must not bypass the cache")
        }
        val useCase = SimilarMovieUseCaseImpl(repo, mainThreadSurrogate, 1L)
        val result = useCase()
        assertIs<Result.Success<List<SimilarMovieShowModel>?>>(result)
        assertEquals(MockedRepository.COUNT, result.data?.size)
    }

    @Test
    fun test_returns_null_when_all_movies_lack_poster_path() = runTest {
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun similarMoviesFirstPage(movieId: Long): Result<List<Movie>, MovieDataException> =
                Result.Success(
                    listOf(
                        Movie(
                            id = 1L,
                            title = "a",
                            overview = "abc",
                            posterPath = null,
                            backdropPath = null,
                            voteAverage = 1.0,
                            genreIds = null,
                        )
                    )
                )
        }
        val useCase = SimilarMovieUseCaseImpl(repo, mainThreadSurrogate, 1L)
        val result = useCase()
        assertIs<Result.Success<List<SimilarMovieShowModel>?>>(result)
        assertNull(result.data)
    }

    @Test
    fun test_returns_error_when_similar_movies_fails() = runTest {
        val failure = MovieDataException.Network(RuntimeException("Network error"))
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun similarMoviesFirstPage(movieId: Long): Result<List<Movie>, MovieDataException> =
                Result.Error(failure)
        }
        val useCase = SimilarMovieUseCaseImpl(repo, mainThreadSurrogate, 1L)
        val result = useCase()
        assertIs<Result.Error<MovieDataException>>(result)
        assertSame(failure, result.error)
    }

    @Test
    fun test_returns_error_when_genre_map_fails() = runTest {
        val failure = MovieDataException.Network(RuntimeException("Genre fetch failed"))
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, MovieDataException> =
                Result.Error(failure)
        }
        val useCase = SimilarMovieUseCaseImpl(repo, mainThreadSurrogate, 1L)
        val result = useCase()
        assertIs<Result.Error<MovieDataException>>(result)
        assertSame(failure, result.error)
    }
}
