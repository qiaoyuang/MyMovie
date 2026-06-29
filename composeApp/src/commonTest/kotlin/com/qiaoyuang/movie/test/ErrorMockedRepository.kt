package com.qiaoyuang.movie.test

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse

/**
 * A [MovieRepository] whose every call fails with [ERROR_MESSAGE], used to drive
 * the error branches of the ViewModels and use cases under test.
 */
internal class ErrorMockedRepository : MovieRepository {

    companion object {
        const val ERROR_MESSAGE = "Mocked network error"
    }

    override suspend fun getMovieGenreList(): Result<List<MovieGenre>, String> = Result.Error(ERROR_MESSAGE)

    override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, String> = Result.Error(ERROR_MESSAGE)

    override suspend fun fetchTopRated(page: Int): Result<MovieResponse, String> = Result.Error(ERROR_MESSAGE)

    override suspend fun movieDetail(movieId: Long): Result<Movie, String> = Result.Error(ERROR_MESSAGE)

    override suspend fun similarMovies(movieId: Long, page: Int): Result<MovieResponse, String> =
        Result.Error(ERROR_MESSAGE)

    override suspend fun fetchMovieGenre(): Result<List<MovieGenre>, String> = Result.Error(ERROR_MESSAGE)

    override suspend fun search(word: String, page: Int): Result<MovieResponse, String> = Result.Error(ERROR_MESSAGE)
}
