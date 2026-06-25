package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.domain.toDomain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class MovieRepositoryImpl(
    private val service: APIService,
    private val defaultDispatcher: CoroutineDispatcher,
) : MovieRepository {

    override suspend infix fun fetchTopRated(page: Int): Result<MovieResponse, String> =
        wrap { service.fetchTopRated(page).toDomain() }

    override suspend fun movieDetail(movieId: Long): Result<Movie, String> =
        wrap { (service movieDetail movieId).toDomain() }

    override suspend fun similarMovies(movieId: Long, page: Int): Result<MovieResponse, String> =
        wrap { service.similarMovies(movieId, page).toDomain() }

    override suspend fun fetchMovieGenre(): Result<List<MovieGenre>, String> =
        wrap { service.fetchMovieGenre().genres.map { it.toDomain() } }

    override suspend fun search(word: String, page: Int): Result<MovieResponse, String> =
        wrap { service.search(word, page).toDomain() }

    private suspend inline fun <T> wrap(crossinline fetch: suspend () -> T): Result<T, String> =
        withContext(defaultDispatcher) {
            try {
                Result.Success(fetch())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(e.message ?: "")
            }
        }

    private var movieGenreList: Result.Success<List<MovieGenre>>? = null
    override suspend fun getMovieGenreList(): Result<List<MovieGenre>, String> = movieGenreList ?: kotlin.run {
        val result = fetchMovieGenre()
        if (result is Result.Success<List<MovieGenre>>)
            movieGenreList = result
        return result
    }

    private var movieGenreMap: Result.Success<IntObjectMap<String>>? = null
    override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, String> = movieGenreMap ?: kotlin.run {
        when (val result = getMovieGenreList()) {
            is Result.Success<List<MovieGenre>> -> {
                val data = result.data
                val map = MutableIntObjectMap<String>(data.size)
                data.forEach { (id, name) ->
                    map[id] = name
                }
                movieGenreMap = Result.Success(map)
                movieGenreMap!!
            }
            is Result.Error<String> -> result
        }
    }
}