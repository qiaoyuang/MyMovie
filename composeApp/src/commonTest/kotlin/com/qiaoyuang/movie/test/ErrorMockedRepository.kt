package com.qiaoyuang.movie.test

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.MovieDataException

/**
 * A [MovieRepository] whose every call fails with [ERROR], used to drive the error branches of
 * the ViewModels, use cases and paging sources under test. A Network failure, because that is
 * the one the UI shows differently.
 */
internal class ErrorMockedRepository : MovieRepository {

    companion object {
        val ERROR: MovieDataException = MovieDataException.Network(RuntimeException("Mocked network error"))
    }

    override suspend fun getMovieGenreList(): Result<List<MovieGenre>, MovieDataException> = Result.Error(ERROR)

    override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, MovieDataException> = Result.Error(ERROR)

    override suspend fun fetchTopRated(page: Int): Result<MovieResponse, MovieDataException> = Result.Error(ERROR)

    override suspend fun movieDetail(movieId: Long): Result<Movie, MovieDataException> = Result.Error(ERROR)

    override suspend fun similarMovies(movieId: Long, page: Int): Result<MovieResponse, MovieDataException> = Result.Error(ERROR)

    override suspend fun fetchMovieGenre(): Result<List<MovieGenre>, MovieDataException> = Result.Error(ERROR)

    override suspend fun search(word: String, page: Int): Result<MovieResponse, MovieDataException> = Result.Error(ERROR)
}
