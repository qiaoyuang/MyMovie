package com.qiaoyuang.movie.model

internal interface APIService {

    companion object {
        // Will move it to resource
        const val KEY = "e4f9e61f6ffd66639d33d3dde7e3159b"

        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val API_KEY_PARAM = "api_key"

        infix fun buildImageUrl(path: String) = "https://image.tmdb.org/t/p/w500$path"
    }

    suspend infix fun fetchTopRated(page: Int = 1): ApiMovieResponse

    suspend infix fun movieDetail(movieId: Long): ApiMovie

    suspend fun similarMovies(movieId: Long, page: Int = 1): ApiMovieResponse

    suspend fun fetchMovieGenre(): ApiMovieGenresResponse

    suspend infix fun search(word: String): ApiMovieResponse
}