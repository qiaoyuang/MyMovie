package com.qiaoyuang.movie.test

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.ApiMovieGenresResponse
import com.qiaoyuang.movie.model.ApiMovieResponse
import com.qiaoyuang.movie.model.MovieGenre
import com.qiaoyuang.movie.model.MovieRepository
import kotlin.String

internal class MockedRepository : MovieRepository {

    companion object {
        const val TOTAL_PAGES = 5
        const val TOTAL_RESULTS = 25
        const val COUNT = 5
        const val GENRE_SIZE = 3
    }

    private fun generateMovie(id: Long): ApiMovie = ApiMovie(
        id = id,
        title = ('a'.code + id.toInt()).toChar().toString(),
        overview = "abc",
        posterPath = "https://xyz",
        backdropPath = "https://uvw",
        voteAverage = id.toDouble().toString(),
        genreIds = listOf(id.toInt() % 3),
    )

    private var point = 1L
    private fun generateMovies(count: Int): List<ApiMovie> = buildList {
        repeat(count) {
            add(generateMovie(point++))
        }
    }

    override suspend fun getMovieGenreList(): List<MovieGenre> = listOf(
        MovieGenre(1, "a"),
        MovieGenre(2, "b"),
        MovieGenre(3, "c")
    )

    override suspend fun getMovieGenreMap(): IntObjectMap<String> = getMovieGenreList().run {
        val map = MutableIntObjectMap<String>(size)
        forEach { (id, name) ->
            map[id] = name
        }
        map
    }

    override suspend fun fetchTopRated(page: Int): ApiMovieResponse = ApiMovieResponse(
        page = page,
        results = generateMovies(COUNT),
        totalPages = TOTAL_PAGES,
        totalResults = TOTAL_RESULTS,
    )

    override suspend fun movieDetail(movieId: Long): ApiMovie = generateMovie(movieId)

    override suspend fun similarMovies(
        movieId: Long,
        page: Int
    ): ApiMovieResponse = ApiMovieResponse(
        page = page,
        results = generateMovies(COUNT),
        totalPages = TOTAL_PAGES,
        totalResults = TOTAL_RESULTS,
    )

    override suspend fun fetchMovieGenre(): ApiMovieGenresResponse = ApiMovieGenresResponse(
        genres = listOf(
            MovieGenre(1, "a"),
            MovieGenre(2, "b"),
            MovieGenre(3, "c")
        ),
    )

    override suspend fun search(word: String): ApiMovieResponse = ApiMovieResponse(
        page = 1,
        results = generateMovies(TOTAL_RESULTS),
        totalPages = TOTAL_PAGES,
        totalResults = TOTAL_RESULTS,
    )

    fun reset() {
        point = 1L
    }
}