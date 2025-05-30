package com.qiaoyuang.movie.model

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class KtorService(private val client: HttpClient) : APIService {

    override suspend infix fun fetchTopRated(page: Int): ApiMovieResponse =
        client.get("movie/top_rated") {
            url.parameters.append("page", page.toString())
        }.body()

    override suspend fun movieDetail(movieId: Long): ApiMovie =
        client.get("movie/$movieId").body()

    override suspend fun similarMovies(movieId: Long, page: Int): ApiMovieResponse =
        client.get("movie/$movieId/similar") {
            url.parameters.append("page", page.toString())
        }.body()

    override suspend fun fetchMovieGenre(): ApiMovieGenresResponse =
        client.get("genre/movie/list").body()

    override suspend fun search(word: String): ApiMovieResponse =
        client.get("search/movie") {
            url.parameters.append("query", word)
        }.body()
}