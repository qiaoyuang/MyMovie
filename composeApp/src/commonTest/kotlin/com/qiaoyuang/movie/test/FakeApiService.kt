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

    override suspend infix fun fetchTopRated(page: Int): ApiMovieResponseDTO = unused()

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

    override suspend fun similarMovies(movieId: Long, page: Int): ApiMovieResponseDTO = unused()

    override suspend fun fetchMovieGenre(): ApiMovieGenresResponseDTO {
        genreCalls++
        failIfOffline()
        return ApiMovieGenresResponseDTO(genres)
    }

    override suspend fun search(word: String, page: Int): ApiMovieResponseDTO = unused()

    private fun failIfOffline() {
        if (offline) throw RuntimeException("offline")
    }

    private fun unused(): Nothing =
        throw AssertionError("this endpoint is not part of the repository's caching policy")
}
