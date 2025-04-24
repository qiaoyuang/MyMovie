package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap

class MovieRepositoryImpl(private val service: APIService) : MovieRepository {

    override suspend infix fun fetchTopRated(page: Int): ApiMovieResponse =
        service fetchTopRated page

    override suspend fun movieDetail(movieId: Long): ApiMovie =
        service movieDetail movieId

    override suspend fun similarMovies(movieId: Long, page: Int): ApiMovieResponse =
        service.similarMovies(movieId, page)

    override suspend fun fetchMovieGenre(): ApiMovieGenresResponse =
        service.fetchMovieGenre()

    override suspend fun search(word: String): ApiMovieResponse =
        service search word

    private var movieGenres: IntObjectMap<String>? = null

    override suspend fun getMovieGenres(): IntObjectMap<String> = movieGenres ?: fetchMovieGenre().let {
        val map = MutableIntObjectMap<String>(it.genres.size)
        it.genres.forEach { (id, name) ->
            map[id] = name
        }
        movieGenres = map
        map
    }
}