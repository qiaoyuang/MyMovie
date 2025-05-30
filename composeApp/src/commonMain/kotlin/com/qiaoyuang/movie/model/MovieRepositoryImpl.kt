package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import kotlin.concurrent.Volatile

internal class MovieRepositoryImpl(private val service: APIService) : MovieRepository {

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

    @Volatile
    private var movieGenreList: List<MovieGenre>? = null
    override suspend fun getMovieGenreList(): List<MovieGenre> = movieGenreList ?: try {
        fetchMovieGenre().genres.also {
            movieGenreList = it
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    @Volatile
    private var movieGenreMap: IntObjectMap<String>? = null
    override suspend fun getMovieGenreMap(): IntObjectMap<String> = movieGenreMap ?: getMovieGenreList().run {
        val map = MutableIntObjectMap<String>(size)
        forEach { (id, name) ->
            map[id] = name
        }
        movieGenreMap = map
        map
    }
}