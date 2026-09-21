package com.qiaoyuang.movie.appfunctions

import com.qiaoyuang.movie.model.APIService
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.qiaoyuang.movie.model.MovieDataException

data class MovieData(
    val id: Long,
    val title: String,
    val overview: String,
    val rating: String?,
    val posterUrl: String?,
)

class MovieDataBridge internal constructor(private val repository: MovieRepository) {

    companion object : KoinComponent {

        /**
         * Wraps the MovieRepository that Koin holds as a `single`, so every caller shares one
         * repository and one genre cache. This used to build its own Json, HttpClient,
         * KtorService and MovieRepositoryImpl, which meant a second cache the UI never saw and
         * a serialization config that could drift from Injection.kt with no compile error.
         *
         * Resolved lazily, so it fails loudly only if something runs before
         * MovieApplication.onCreate() has started Koin — which is what we want.
         */
        val instance by lazy { MovieDataBridge(get()) }
    }

    suspend fun searchMovies(query: String, page: Int): List<MovieData>? =
        repository.search(query, page).toMovieData()

    suspend fun getTopRatedMovies(page: Int): List<MovieData>? =
        repository.fetchTopRated(page).toMovieData()

    suspend fun getSimilarMovies(movieId: Long, page: Int): List<MovieData>? =
        repository.similarMovies(movieId, page).toMovieData()
}

private fun Result<MovieResponse, MovieDataException>.toMovieData(): List<MovieData>? =
    (this as? Result.Success<MovieResponse>)?.data?.results?.map { it.toMovieData() }

private fun Movie.toMovieData() = MovieData(
    id = id,
    title = title,
    overview = overview,
    rating = voteAverage,
    posterUrl = posterPath?.let { APIService buildImageUrl it },
)
