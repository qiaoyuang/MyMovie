package com.qiaoyuang.movie.test

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.model.MovieDataException
import com.qiaoyuang.movie.model.MovieRepositoryImpl
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.local.CACHE_TTL_MILLIS
import com.qiaoyuang.movie.model.local.similarListKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The repository's own caching policy: movie details and the genre catalogue. */
class MovieRepositoryImplTest {

    private val nowMillis = 1_700_000_000_000L

    private fun movie(id: Long) = Movie(
        id = id,
        title = "cached $id",
        overview = "overview $id",
        posterPath = "/poster$id",
        backdropPath = null,
        voteAverage = 8.0,
        genreIds = listOf(12),
    )

    private fun repository(
        service: FakeApiService,
        local: FakeMovieLocalDataSource,
        now: Long = nowMillis,
    ) = MovieRepositoryImpl(service, local, Dispatchers.Unconfined, now = { now })

    // ---- movie detail ----

    @Test
    fun aCachedMovieIsReturnedWithoutARequest() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        local.upsertMovies(listOf(movie(550L)))

        val result = repository(service, local).movieDetail(550L)

        assertIs<Result.Success<Movie>>(result)
        assertEquals("cached 550", result.data.title)
        assertEquals(0, service.movieDetailCalls)
    }

    @Test
    fun aMissIsFetchedAndThenCached() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        val repository = repository(service, local)

        val first = repository.movieDetail(550L)
        assertIs<Result.Success<Movie>>(first)
        assertEquals("fetched 550", first.data.title)
        assertEquals(1, service.movieDetailCalls)

        // Second time it comes out of the cache the first call populated.
        val second = repository.movieDetail(550L)
        assertIs<Result.Success<Movie>>(second)
        assertEquals("fetched 550", second.data.title)
        assertEquals(1, service.movieDetailCalls)
    }

    @Test
    fun genreIdsSurviveTheRoundTripThroughTheCache() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        val repository = repository(service, local)

        repository.movieDetail(550L)
        val cached = repository.movieDetail(550L)

        assertIs<Result.Success<Movie>>(cached)
        assertEquals(listOf(28), cached.data.genreIds)
    }

    @Test
    fun aMissWithNoNetworkReportsTheTypedFailure() = runTest {
        val service = FakeApiService().apply { offline = true }

        val result = repository(service, FakeMovieLocalDataSource()).movieDetail(550L)

        assertIs<Result.Error<MovieDataException>>(result)
        assertIs<MovieDataException.Unknown>(result.error)
    }

    @Test
    fun aCachedMovieOpensEvenWithNoNetwork() = runTest {
        val service = FakeApiService().apply { offline = true }
        val local = FakeMovieLocalDataSource()
        local.upsertMovies(listOf(movie(550L)))

        val result = repository(service, local).movieDetail(550L)

        assertIs<Result.Success<Movie>>(result)
        assertEquals(0, service.movieDetailCalls)
    }

    // ---- similar movies, first page ----

    @Test
    fun theFirstPageOfSimilarMoviesIsFetchedOnceAndThenCached() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        val repository = repository(service, local)

        val first = repository.similarMoviesFirstPage(550L)
        val second = repository.similarMoviesFirstPage(550L)

        assertIs<Result.Success<List<Movie>>>(first)
        assertIs<Result.Success<List<Movie>>>(second)
        assertEquals(first.data.map(Movie::id), second.data.map(Movie::id))
        assertEquals(listOf(1), service.similarPagesRequested)
    }

    /**
     * What makes the detail strip and the paged "all similar movies" screen share one fetch:
     * the strip leaves a cursor behind, so the mediator opens on a fresh list and continues
     * from page two instead of re-reading page one.
     */
    @Test
    fun theFirstPageLeavesACursorThePagedScreenCanContinueFrom() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()

        repository(service, local).similarMoviesFirstPage(550L)

        val cursor = local.cursorOf(similarListKey(550L))
        assertEquals(2, cursor?.nextPage)
        assertEquals(3, cursor?.totalPages)
        assertEquals(nowMillis, cursor?.lastRefreshedAt)
    }

    @Test
    fun eachMovieGetsItsOwnCachedSimilarList() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        val repository = repository(service, local)

        repository.similarMoviesFirstPage(550L)
        repository.similarMoviesFirstPage(680L)

        assertEquals(listOf(1, 1), service.similarPagesRequested)
        assertEquals(20, local.positionsIn(similarListKey(550L)).size)
        assertEquals(20, local.positionsIn(similarListKey(680L)).size)
    }

    @Test
    fun aStaleSimilarListIsRefetched() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        repository(service, local).similarMoviesFirstPage(550L)

        repository(service, local, now = nowMillis + CACHE_TTL_MILLIS).similarMoviesFirstPage(550L)

        assertEquals(listOf(1, 1), service.similarPagesRequested)
    }

    @Test
    fun aStaleSimilarListIsStillServedWhenTheFetchFails() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        repository(service, local).similarMoviesFirstPage(550L)
        service.offline = true

        val result = repository(service, local, now = nowMillis + CACHE_TTL_MILLIS)
            .similarMoviesFirstPage(550L)

        assertIs<Result.Success<List<Movie>>>(result)
        assertEquals(20, result.data.size)
    }

    @Test
    fun anEmptySimilarListAndNoNetworkIsAFailure() = runTest {
        val service = FakeApiService().apply { offline = true }

        val result = repository(service, FakeMovieLocalDataSource()).similarMoviesFirstPage(550L)

        assertIs<Result.Error<MovieDataException>>(result)
    }

    // ---- genre catalogue ----

    @Test
    fun theCatalogueIsFetchedOnceAndThenServedFromTheTable() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        val repository = repository(service, local)

        val first = repository.getMovieGenreList()
        val second = repository.getMovieGenreList()

        assertIs<Result.Success<List<MovieGenre>>>(first)
        assertEquals(listOf(MovieGenre(28, "Action"), MovieGenre(12, "Adventure")), first.data)
        assertEquals(first.data, (second as Result.Success<List<MovieGenre>>).data)
        assertEquals(1, service.genreCalls)
        assertEquals(nowMillis, local.genresRefreshedAt())
    }

    @Test
    fun aStaleCatalogueIsRefetched() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        local.seedGenres(listOf(MovieGenre(99, "Retired")), refreshedAt = nowMillis)

        val result = repository(service, local, now = nowMillis + CACHE_TTL_MILLIS).getMovieGenreList()

        assertIs<Result.Success<List<MovieGenre>>>(result)
        assertEquals(1, service.genreCalls)
        // Replaced, not merged — the retired genre is gone.
        assertTrue(result.data.none { it.id == 99 })
    }

    @Test
    fun aFreshCatalogueIsNotRefetched() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        local.seedGenres(listOf(MovieGenre(99, "Cached")), refreshedAt = nowMillis)

        val result = repository(service, local, now = nowMillis + CACHE_TTL_MILLIS - 1)
            .getMovieGenreList()

        assertIs<Result.Success<List<MovieGenre>>>(result)
        assertEquals(listOf(MovieGenre(99, "Cached")), result.data)
        assertEquals(0, service.genreCalls)
    }

    /** Genre names are what make a cached similar-movies row readable, so stale beats nothing. */
    @Test
    fun aStaleCatalogueIsStillServedWhenTheFetchFails() = runTest {
        val service = FakeApiService().apply { offline = true }
        val local = FakeMovieLocalDataSource()
        local.seedGenres(listOf(MovieGenre(99, "Cached")), refreshedAt = nowMillis)

        val result = repository(service, local, now = nowMillis + CACHE_TTL_MILLIS).getMovieGenreList()

        assertIs<Result.Success<List<MovieGenre>>>(result)
        assertEquals(listOf(MovieGenre(99, "Cached")), result.data)
        assertEquals(1, service.genreCalls)
    }

    @Test
    fun anEmptyCatalogueAndNoNetworkIsAFailure() = runTest {
        val service = FakeApiService().apply { offline = true }

        val result = repository(service, FakeMovieLocalDataSource()).getMovieGenreList()

        assertIs<Result.Error<MovieDataException>>(result)
    }

    @Test
    fun aFailedFetchIsNotCachedSoALaterCallerRetries() = runTest {
        val service = FakeApiService().apply { offline = true }
        val local = FakeMovieLocalDataSource()
        val repository = repository(service, local)

        assertIs<Result.Error<MovieDataException>>(repository.getMovieGenreList())
        assertNull(local.genresRefreshedAt())

        service.offline = false
        assertIs<Result.Success<List<MovieGenre>>>(repository.getMovieGenreList())
        assertEquals(2, service.genreCalls)
    }

    /**
     * The reason the mutex spans the whole check-fetch-store rather than each step: guarding the
     * steps separately would let every concurrent caller find an empty table and fire its own
     * request.
     */
    @Test
    fun concurrentCallersShareOneRequest() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        val repository = repository(service, local)

        val results = List(8) { async { repository.getMovieGenreList() } }.awaitAll()

        assertEquals(1, service.genreCalls)
        assertTrue(results.all { it is Result.Success<List<MovieGenre>> })
    }

    @Test
    fun theGenreMapIsDerivedFromTheSameCachedCatalogue() = runTest {
        val service = FakeApiService()
        val local = FakeMovieLocalDataSource()
        val repository = repository(service, local)

        repository.getMovieGenreList()
        val map = repository.getMovieGenreMap()

        assertIs<Result.Success<IntObjectMap<String>>>(map)
        assertEquals("Action", map.data[28])
        assertEquals("Adventure", map.data[12])
        assertNull(map.data[999])
        assertEquals(1, service.genreCalls)
    }

    /** getMovieGenreMap takes the same non-reentrant mutex, so it must not deadlock. */
    @Test
    fun theGenreMapCanBeTheFirstCaller() = runTest {
        val service = FakeApiService()

        val map = repository(service, FakeMovieLocalDataSource()).getMovieGenreMap()

        assertIs<Result.Success<IntObjectMap<String>>>(map)
        assertEquals("Action", map.data[28])
        assertEquals(1, service.genreCalls)
    }
}
