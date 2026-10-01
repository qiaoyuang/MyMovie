package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.domain.toDomain
import com.qiaoyuang.movie.model.local.MovieLocalDataSource
import com.qiaoyuang.movie.model.local.currentTimeMillis
import com.qiaoyuang.movie.model.local.isStale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Owns the caching policy, as the architecture guide puts it — callers ask for a movie, not for
 * a request. Paged lists are the exception: their cache is written by MovieRemoteMediator, which
 * is where Paging requires that decision to live.
 */
internal class MovieRepositoryImpl(
    private val service: APIService,
    private val local: MovieLocalDataSource,
    private val defaultDispatcher: CoroutineDispatcher,
    private val now: () -> Long = ::currentTimeMillis,
) : MovieRepository {

    override suspend infix fun fetchTopRated(page: Int): Result<MovieResponse, MovieDataException> =
        wrap { service.fetchTopRated(page).toDomain() }

    /**
     * Cache first, network only on a miss. TMDB's detail endpoint returns the same payload the
     * list endpoints do, so a movie the user reached from a list is already as good as fetched —
     * and the cached hit is what makes this screen open with no network.
     *
     * Freshness rides on the lists rather than on a per-movie timestamp, because refreshing a
     * list rewrites every movie in it. The gap that leaves: a movie only ever reached directly,
     * without belonging to any cached list, is never re-read. Giving MovieEntity its own stamp
     * would close it, at the cost of a column every list write has to maintain.
     */
    override suspend fun movieDetail(movieId: Long): Result<Movie, MovieDataException> {
        local.movie(movieId)?.let { return Result.Success(it) }
        val fetched = wrap { (service movieDetail movieId).toDomain() }
        if (fetched is Result.Success<Movie>) local.upsertMovies(listOf(fetched.data))
        return fetched
    }

    override suspend fun similarMovies(movieId: Long, page: Int): Result<MovieResponse, MovieDataException> =
        wrap { service.similarMovies(movieId, page).toDomain() }

    override suspend fun fetchMovieGenre(): Result<List<MovieGenre>, MovieDataException> =
        wrap { service.fetchMovieGenre().genres.map { it.toDomain() } }

    override suspend fun search(word: String, page: Int): Result<MovieResponse, MovieDataException> =
        wrap { service.search(word, page).toDomain() }

    /**
     * Ktor's engines do their own network I/O on their own threads, but the response pipeline
     * does not switch dispatchers: KotlinxSerializationConverter.deserialize() decodes on
     * whatever context calls body(). Measured on a desktop JVM, decoding one 12 KB TMDB page
     * costs ~55us against ~0.5us for toDomain() — so the parse, not the mapping, is what has
     * to stay off the main thread. Default rather than IO because none of this blocks; the
     * thread is released back to the pool while the request is suspended.
     */
    private suspend inline fun <T> wrap(crossinline fetch: suspend () -> T): Result<T, MovieDataException> =
        withContext(defaultDispatcher) {
            try {
                Result.Success(fetch())
            } catch (e: CancellationException) {
                // Must propagate: reporting it as a failure would show an error for a load the
                // user simply navigated away from.
                throw e
            } catch (e: Exception) {
                // Used to be Result.Error(e.message), which threw away the type and carried the
                // request URL — api_key included — up through every layer as "the error".
                Result.Error(e.toMovieDataException())
            }
        }

    /**
     * One Mutex guards the whole check-fetch-store. Reading the cache and writing it back are
     * separated by a network call, so the sequence has to be atomic as a whole — guarding each
     * step on its own would still let N concurrent callers fire N requests.
     *
     * The lock is deliberately held across that call: that is what makes a second caller wait
     * and then find the catalogue already there. Per-caller cancellation stays correct, because
     * each caller runs its own fetch in its own context — if one is cancelled mid-flight the
     * lock is released and the next caller simply retries.
     *
     * The two in-memory copies this used to keep are gone. The table is the cache now, so the
     * catalogue survives process death, and nineteen rows are not worth a second tier.
     */
    private val genreMutex = Mutex()

    override suspend fun getMovieGenreList(): Result<List<MovieGenre>, MovieDataException> =
        genreMutex.withLock { loadGenreList() }

    override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, MovieDataException> =
        genreMutex.withLock {
            when (val result = loadGenreList()) {
                is Result.Success<List<MovieGenre>> -> Result.Success(result.data.toGenreMap())
                is Result.Error<MovieDataException> -> result
            }
        }

    /**
     * Must only be called while holding [genreMutex] — kotlinx.coroutines' Mutex is not
     * reentrant, so going through the public getter would deadlock.
     */
    private suspend fun loadGenreList(): Result<List<MovieGenre>, MovieDataException> {
        val cached = local.genres()
        val refreshedAt = local.genresRefreshedAt()
        if (cached.isNotEmpty() && refreshedAt != null && !isStale(refreshedAt, now()))
            return Result.Success(cached)

        return when (val fetched = fetchMovieGenre()) {
            is Result.Success<List<MovieGenre>> -> fetched.also {
                local.replaceGenres(it.data, refreshedAt = now())
            }
            // A stale catalogue still beats showing no genre names at all, so the failure is
            // only reported when there is nothing cached to fall back on.
            is Result.Error<MovieDataException> ->
                if (cached.isNotEmpty()) Result.Success(cached) else fetched
        }
    }

    /**
     * Rebuilt per call rather than memoised: nineteen entries, and the one caller
     * (SimilarMovieUseCase) caches its own result anyway.
     */
    private fun List<MovieGenre>.toGenreMap(): IntObjectMap<String> {
        val map = MutableIntObjectMap<String>(size)
        forEach { (id, name) -> map[id] = name }
        return map
    }
}
