package com.qiaoyuang.movie.appfunctions

import com.qiaoyuang.movie.model.APIService
import com.qiaoyuang.movie.model.APIService.Companion.API_KEY_PARAM
import com.qiaoyuang.movie.model.APIService.Companion.BASE_URL
import com.qiaoyuang.movie.model.APIService.Companion.KEY
import com.qiaoyuang.movie.model.KtorService
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.MovieRepositoryImpl
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.ktorEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

data class MovieData(
    val id: Long,
    val title: String,
    val overview: String,
    val rating: String?,
    val posterUrl: String?,
)

class MovieDataBridge internal constructor(private val repository: MovieRepository) {

    companion object {
        @Volatile
        private var instance: MovieDataBridge? = null

        fun getInstance(): MovieDataBridge = instance ?: synchronized(this) {
            instance ?: create().also { instance = it }
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun create(): MovieDataBridge {
            val json = Json {
                explicitNulls = false
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
                allowTrailingComma = true
            }
            val client = HttpClient(ktorEngine) {
                expectSuccess = true
                install(ContentNegotiation) { json(json) }
                defaultRequest {
                    url(BASE_URL)
                    url.parameters.append(API_KEY_PARAM, KEY)
                }
            }
            return MovieDataBridge(MovieRepositoryImpl(KtorService(client), Dispatchers.Default))
        }
    }

    suspend fun searchMovies(query: String, page: Int): List<MovieData>? = withContext(Dispatchers.IO) {
        (repository.search(query, page) as? Result.Success<MovieResponse>)?.data?.results?.map { movie ->
            MovieData(
                id = movie.id,
                title = movie.title,
                overview = movie.overview,
                rating = movie.voteAverage,
                posterUrl = movie.posterPath?.let { APIService buildImageUrl it },
            )
        }
    }

    suspend fun getTopRatedMovies(page: Int): List<MovieData>? = withContext(Dispatchers.IO) {
        (repository.fetchTopRated(page) as? Result.Success<MovieResponse>)?.data?.results?.map { movie ->
            MovieData(
                id = movie.id,
                title = movie.title,
                overview = movie.overview,
                rating = movie.voteAverage,
                posterUrl = movie.posterPath?.let { APIService buildImageUrl it },
            )
        }
    }

    suspend fun getSimilarMovies(movieId: Long, page: Int): List<MovieData>? =
        (repository.similarMovies(movieId, page) as? Result.Success<MovieResponse>)?.data?.results?.map { movie ->
            MovieData(
                id = movie.id,
                title = movie.title,
                overview = movie.overview,
                rating = movie.voteAverage,
                posterUrl = movie.posterPath?.let { APIService buildImageUrl it },
            )
        }
}
