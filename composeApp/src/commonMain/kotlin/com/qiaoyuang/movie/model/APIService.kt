package com.qiaoyuang.movie.model

import com.qiaoyuang.movie.model.dto.ApiMovieDTO
import com.qiaoyuang.movie.model.dto.ApiMovieGenresResponseDTO
import com.qiaoyuang.movie.model.dto.ApiMovieResponseDTO
import io.mockative.Mockable

@Mockable
internal interface APIService {

    companion object {
        // Will move it to resource
        const val KEY = "e4f9e61f6ffd66639d33d3dde7e3159b"

        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val API_KEY_PARAM = "api_key"

        infix fun buildImageUrl(path: String) = "https://image.tmdb.org/t/p/w500$path"
    }

    suspend infix fun fetchTopRated(page: Int = 1): ApiMovieResponseDTO

    suspend infix fun movieDetail(movieId: Long): ApiMovieDTO

    suspend fun similarMovies(movieId: Long, page: Int = 1): ApiMovieResponseDTO

    suspend fun fetchMovieGenre(): ApiMovieGenresResponseDTO

    suspend fun search(word: String, page: Int): ApiMovieResponseDTO
}