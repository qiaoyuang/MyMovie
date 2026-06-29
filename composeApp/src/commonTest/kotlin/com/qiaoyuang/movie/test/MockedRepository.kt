package com.qiaoyuang.movie.test

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse

internal class MockedRepository : MovieRepository {

    companion object {
        const val TOTAL_PAGES = 5
        const val TOTAL_RESULTS = 25
        const val COUNT = 5
        const val GENRE_SIZE = 3
    }

    private fun generateMovie(id: Long): Movie = Movie(
        id = id,
        title = ('a'.code + id.toInt()).toChar().toString(),
        overview = "abc",
        posterPath = "https://xyz",
        backdropPath = "https://uvw",
        voteAverage = id.toDouble().toString(),
        genreIds = listOf(id.toInt() % 3),
    )

    private var point = 1L
    private fun generateMovies(count: Int): List<Movie> = buildList {
        repeat(count) {
            add(generateMovie(point++))
        }
    }

    override suspend fun getMovieGenreList(): Result<List<MovieGenre>, String> = Result.Success(
        listOf(
            MovieGenre(1, "a"),
            MovieGenre(2, "b"),
            MovieGenre(3, "c"),
        )
    )

    override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, String> {
        val genres = listOf(MovieGenre(1, "a"), MovieGenre(2, "b"), MovieGenre(3, "c"))
        val map = MutableIntObjectMap<String>(genres.size)
        genres.forEach { map[it.id] = it.name }
        return Result.Success(map)
    }

    override suspend fun fetchTopRated(page: Int): Result<MovieResponse, String> = Result.Success(
        MovieResponse(
            page = page,
            results = generateMovies(COUNT),
            totalPages = TOTAL_PAGES,
        )
    )

    override suspend fun movieDetail(movieId: Long): Result<Movie, String> =
        Result.Success(generateMovie(movieId))

    override suspend fun similarMovies(
        movieId: Long,
        page: Int,
    ): Result<MovieResponse, String> = Result.Success(
        MovieResponse(
            page = page,
            results = generateMovies(COUNT),
            totalPages = TOTAL_PAGES,
        )
    )

    override suspend fun fetchMovieGenre(): Result<List<MovieGenre>, String> = Result.Success(
        listOf(
            MovieGenre(1, "a"),
            MovieGenre(2, "b"),
            MovieGenre(3, "c"),
        )
    )

    override suspend fun search(word: String, page: Int): Result<MovieResponse, String> =
        Result.Success(
            MovieResponse(
                page = 1,
                results = generateMovies(TOTAL_RESULTS),
                totalPages = TOTAL_PAGES,
            )
        )
}