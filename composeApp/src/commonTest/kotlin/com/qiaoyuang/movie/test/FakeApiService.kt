package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.model.APIService
import com.qiaoyuang.movie.model.dto.ApiMovieDTO
import com.qiaoyuang.movie.model.dto.ApiMovieGenresResponseDTO
import com.qiaoyuang.movie.model.dto.ApiMovieResponseDTO
import com.qiaoyuang.movie.model.dto.MovieGenreDTO

/**
 * Hand-written rather than mocked, because these tests are about how often the repository
 * reaches the network: the call counters are the assertion, and a failure is injected by
 * flipping [offline] rather than by scripting a mock.
 */
internal class FakeApiService(
    private val genres: List<MovieGenreDTO> = listOf(
        MovieGenreDTO(28, "Action"),
        MovieGenreDTO(12, "Adventure"),
    ),
) : APIService {

    /** Every call throws while set, standing in for having no network. */
    var offline = false

    var movieDetailCalls = 0
        private set
    var genreCalls = 0
        private set
    val similarPagesRequested = mutableListOf<Int>()
    val topRatedPagesRequested = mutableListOf<Int>()

    /** How many pages the top-rated list has. */
    var topRatedTotalPages = 4

    /** The ids on each top-rated page; by default page n holds (n-1)*20+1 .. n*20. */
    var topRatedIds: (page: Int) -> List<Long> = { page -> ((page - 1L) * 20 + 1..page * 20L).toList() }

    override suspend infix fun fetchTopRated(page: Int): ApiMovieResponseDTO {
        topRatedPagesRequested += page
        failIfOffline()
        val ids = topRatedIds(page)
        return ApiMovieResponseDTO(
            page = page,
            results = ids.map { id -> movie(id, title = "top $id", genreIds = listOf(28, 12)) },
            totalPages = topRatedTotalPages,
            totalResults = topRatedTotalPages * 20,
        )
    }

    override suspend infix fun movieDetail(movieId: Long): ApiMovieDTO {
        movieDetailCalls++
        failIfOffline()
        return ApiMovieDTO(
            id = movieId,
            title = "fetched $movieId",
            overview = "overview $movieId",
            posterPath = "/poster$movieId",
            backdropPath = null,
            voteAverage = 7.5,
            genreIds = listOf(28),
        )
    }

    override suspend fun similarMovies(movieId: Long, page: Int): ApiMovieResponseDTO {
        similarPagesRequested += page
        failIfOffline()
        val start = (page - 1L) * 20 + 1
        return ApiMovieResponseDTO(
            page = page,
            results = (start until start + 20).map { id -> movie(id, title = "similar $id", genreIds = listOf(12)) },
            totalPages = 3,
            totalResults = 60,
        )
    }

    override suspend fun fetchMovieGenre(): ApiMovieGenresResponseDTO {
        genreCalls++
        failIfOffline()
        return ApiMovieGenresResponseDTO(genres)
    }

    override suspend fun search(word: String, page: Int): ApiMovieResponseDTO = unused()

    private fun movie(id: Long, title: String, genreIds: List<Int>) = ApiMovieDTO(
        id = id,
        title = title,
        overview = "overview $id",
        posterPath = "/poster$id",
        backdropPath = null,
        voteAverage = 6.0,
        genreIds = genreIds,
    )

    private fun failIfOffline() {
        if (offline) throw RuntimeException("offline")
    }

    private fun unused(): Nothing =
        throw AssertionError("this endpoint is not part of the repository's caching policy")
}
