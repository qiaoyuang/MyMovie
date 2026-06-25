package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse
import io.mockative.Mockable

@Mockable
internal interface MovieRepository {

    suspend infix fun fetchTopRated(page: Int = 1): Result<MovieResponse, String>

    suspend fun movieDetail(movieId: Long): Result<Movie, String>

    suspend fun similarMovies(movieId: Long, page: Int = 1): Result<MovieResponse, String>

    suspend fun fetchMovieGenre(): Result<List<MovieGenre>, String>

    suspend fun search(word: String, page: Int): Result<MovieResponse, String>

    suspend fun getMovieGenreList(): Result<List<MovieGenre>, String>

    suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, String>
}
