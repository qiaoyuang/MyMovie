package com.qiaoyuang.movie.test

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.domain.SimilarMovieUseCaseImpl
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.SimilarMovieShowModel
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class SimilarMovieUseCaseImplTest : BasicTest() {

    @Test
    fun test_returns_success_with_movie_list() = runTest {
        val useCase = SimilarMovieUseCaseImpl(MockedRepository(), mainThreadSurrogate, 1L)
        val result = useCase()
        assertIs<Result.Success<List<SimilarMovieShowModel>?>>(result)
        assertEquals(MockedRepository.COUNT, result.data?.size)
    }

    @Test
    fun test_caches_result_on_second_invocation() = runTest {
        val useCase = SimilarMovieUseCaseImpl(MockedRepository(), mainThreadSurrogate, 1L)
        val first = useCase()
        val second = useCase()
        assertSame(first, second)
    }

    @Test
    fun test_returns_null_when_all_movies_lack_poster_path() = runTest {
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun similarMovies(movieId: Long, page: Int): Result<MovieResponse, String> =
                Result.Success(
                    MovieResponse(
                        page = 1,
                        results = listOf(
                            Movie(
                                id = 1L,
                                title = "a",
                                overview = "abc",
                                posterPath = null,
                                backdropPath = null,
                                voteAverage = "1.0",
                                genreIds = null,
                            )
                        ),
                        totalPages = 1,
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
        val errorMessage = "Network error"
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun similarMovies(movieId: Long, page: Int): Result<MovieResponse, String> =
                Result.Error(errorMessage)
        }
        val useCase = SimilarMovieUseCaseImpl(repo, mainThreadSurrogate, 1L)
        val result = useCase()
        assertIs<Result.Error<String>>(result)
        assertEquals(errorMessage, result.error)
    }

    @Test
    fun test_returns_error_when_genre_map_fails() = runTest {
        val errorMessage = "Genre fetch failed"
        val repo = object : MovieRepository by MockedRepository() {
            override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, String> =
                Result.Error(errorMessage)
        }
        val useCase = SimilarMovieUseCaseImpl(repo, mainThreadSurrogate, 1L)
        val result = useCase()
        assertIs<Result.Error<String>>(result)
        assertEquals(errorMessage, result.error)
    }
}
