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

    /** Always a request. MovieRemoteMediator is the thing that fills the cache, so it cannot read it. */
    suspend fun similarMovies(movieId: Long, page: Int = 1): Result<MovieResponse, MovieDataException>

    /**
     * The first page of similar movies, cache first — what the detail screen's strip shows.
     * Shares the cached list the paged "all similar movies" screen pages through, so whichever
     * of the two opens first fills it for the other.
     */
    suspend fun similarMoviesFirstPage(movieId: Long): Result<List<Movie>, MovieDataException>

    suspend fun fetchMovieGenre(): Result<List<MovieGenre>, MovieDataException>

    suspend fun search(word: String, page: Int): Result<MovieResponse, MovieDataException>

    suspend fun getMovieGenreList(): Result<List<MovieGenre>, MovieDataException>

    suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, MovieDataException>
}
