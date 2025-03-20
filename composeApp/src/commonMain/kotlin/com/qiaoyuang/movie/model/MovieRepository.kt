package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object MovieRepository : KoinComponent {

    private val ktorService = get<KtorService>()

    suspend fun fetchTopRated(page: Int = 1): ApiMovieResponse =
        ktorService.client.get("movie/top_rated") {
            url.parameters.append("page", page.toString())
        }.body()

    suspend fun movieDetail(movieId: Long): ApiMovie =
        ktorService.client.get("movie/$movieId").body()

    suspend fun similarMovies(movieId: Long, page: Int = 1): ApiMovieResponse =
        ktorService.client.get("movie/$movieId/similar") {
            url.parameters.append("page", page.toString())
        }.body()

    suspend fun fetchMovieGenre(): ApiMovieGenresResponse =
        ktorService.client.get("genre/movie/list").body()

    suspend fun search(word: String): ApiMovieResponse =
        ktorService.client.get("search/movie") {
            url.parameters.append("query", word)
        }.body()

    private var movieGenres: IntObjectMap<String>? = null

    suspend fun getMovieGenres(): IntObjectMap<String> = movieGenres ?: fetchMovieGenre().let {
        val map = MutableIntObjectMap<String>(it.genres.size)
        it.genres.forEach { (id, name) ->
            map[id] = name
        }
        movieGenres = map
        map
    }
}