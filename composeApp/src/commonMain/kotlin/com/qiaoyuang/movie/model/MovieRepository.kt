package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse
import io.mockative.Mockable

@Mockable
internal interface MovieRepository {

    suspend infix fun fetchTopRated(page: Int = 1): Result<MovieResponse, MovieDataException>

    suspend fun movieDetail(movieId: Long): Result<Movie, MovieDataException>

    suspend fun similarMovies(movieId: Long, page: Int = 1): Result<MovieResponse, MovieDataException>

    suspend fun fetchMovieGenre(): Result<List<MovieGenre>, MovieDataException>

    suspend fun search(word: String, page: Int): Result<MovieResponse, MovieDataException>

    suspend fun getMovieGenreList(): Result<List<MovieGenre>, MovieDataException>

    suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, MovieDataException>
}
