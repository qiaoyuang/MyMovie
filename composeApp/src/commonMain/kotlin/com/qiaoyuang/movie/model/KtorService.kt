package com.qiaoyuang.movie.model

import com.qiaoyuang.movie.model.dto.ApiMovieDTO
import com.qiaoyuang.movie.model.dto.ApiMovieGenresResponseDTO
import com.qiaoyuang.movie.model.dto.ApiMovieResponseDTO
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class KtorService(private val client: HttpClient) : APIService {

    override suspend infix fun fetchTopRated(page: Int): ApiMovieResponseDTO =
        client.get("movie/top_rated") {
            url.parameters.append("page", page.toString())
        }.body()

    override suspend fun movieDetail(movieId: Long): ApiMovieDTO =
        client.get("movie/$movieId").body()

    override suspend fun similarMovies(movieId: Long, page: Int): ApiMovieResponseDTO =
        client.get("movie/$movieId/similar") {
            url.parameters.append("page", page.toString())
        }.body()

    override suspend fun fetchMovieGenre(): ApiMovieGenresResponseDTO =
        client.get("genre/movie/list").body()

    override suspend fun search(word: String, page: Int): ApiMovieResponseDTO =
        client.get("search/movie") {
            with(url.parameters) {
                append("query", word)
                append("page", page.toString())
            }
        }.body()
}