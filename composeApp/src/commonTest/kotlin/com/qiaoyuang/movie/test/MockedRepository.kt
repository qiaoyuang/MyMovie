package com.qiaoyuang.movie.test

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import com.qiaoyuang.movie.model.dto.ApiMovieDTO
import com.qiaoyuang.movie.model.dto.ApiMovieGenresResponseDTO
import com.qiaoyuang.movie.model.dto.ApiMovieResponseDTO
import com.qiaoyuang.movie.model.dto.MovieGenreDTO
import com.qiaoyuang.movie.model.MovieRepository
import kotlin.String

internal class MockedRepository : MovieRepository {

    companion object {
        const val TOTAL_PAGES = 5
        const val TOTAL_RESULTS = 25
        const val COUNT = 5
        const val GENRE_SIZE = 3
    }

    private fun generateMovie(id: Long): ApiMovieDTO = ApiMovieDTO(
        id = id,
        title = ('a'.code + id.toInt()).toChar().toString(),
        overview = "abc",
        posterPath = "https://xyz",
        backdropPath = "https://uvw",
        voteAverage = id.toDouble().toString(),
        genreIds = listOf(id.toInt() % 3),
    )

    private var point = 1L
    private fun generateMovies(count: Int): List<ApiMovieDTO> = buildList {
        repeat(count) {
            add(generateMovie(point++))
        }
    }

    override suspend fun getMovieGenreList(): List<MovieGenreDTO> = listOf(
        MovieGenreDTO(1, "a"),
        MovieGenreDTO(2, "b"),
        MovieGenreDTO(3, "c")
    )

    override suspend fun getMovieGenreMap(): IntObjectMap<String> = getMovieGenreList().run {
        val map = MutableIntObjectMap<String>(size)
        forEach { (id, name) ->
            map[id] = name
        }
        map
    }

    override suspend fun fetchTopRated(page: Int): ApiMovieResponseDTO = ApiMovieResponseDTO(
        page = page,
        results = generateMovies(COUNT),
        totalPages = TOTAL_PAGES,
        totalResults = TOTAL_RESULTS,
    )

    override suspend fun movieDetail(movieId: Long): ApiMovieDTO = generateMovie(movieId)

    override suspend fun similarMovies(
        movieId: Long,
        page: Int
    ): ApiMovieResponseDTO = ApiMovieResponseDTO(
        page = page,
        results = generateMovies(COUNT),
        totalPages = TOTAL_PAGES,
        totalResults = TOTAL_RESULTS,
    )

    override suspend fun fetchMovieGenre(): ApiMovieGenresResponseDTO = ApiMovieGenresResponseDTO(
        genres = listOf(
            MovieGenreDTO(1, "a"),
            MovieGenreDTO(2, "b"),
            MovieGenreDTO(3, "c")
        ),
    )

    override suspend fun search(word: String, page: Int): ApiMovieResponseDTO = ApiMovieResponseDTO(
        page = 1,
        results = generateMovies(TOTAL_RESULTS),
        totalPages = TOTAL_PAGES,
        totalResults = TOTAL_RESULTS,
    )

    fun reset() {
        point = 1L
    }
}