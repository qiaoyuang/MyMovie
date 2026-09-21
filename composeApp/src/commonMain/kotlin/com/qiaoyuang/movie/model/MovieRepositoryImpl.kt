package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.domain.toDomain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class MovieRepositoryImpl(
    private val service: APIService,
    private val defaultDispatcher: CoroutineDispatcher,
) : MovieRepository {

    override suspend infix fun fetchTopRated(page: Int): Result<MovieResponse, MovieDataException> =
        wrap { service.fetchTopRated(page).toDomain() }

    override suspend fun movieDetail(movieId: Long): Result<Movie, MovieDataException> =
        wrap { (service movieDetail movieId).toDomain() }

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
     * One Mutex guards both genre caches. Reading the cache and writing it back are separated
     * by a network call, so the read-fetch-write has to be atomic as a whole — protecting each
     * field access on its own would still let N concurrent callers fire N requests.
     *
     * The lock is deliberately held across that network call: that is what makes a second
     * caller wait and then find the cache populated. It also keeps per-caller cancellation
     * correct, since each caller runs its own fetch in its own coroutine context — if one is
     * cancelled mid-flight the lock is released and the next caller simply retries.
     *
     * A single Mutex rather than one per cache, because getMovieGenreMap() derives from the
     * list; two locks would mean maintaining a lock ordering by hand.
     */
    private val genreMutex = Mutex()

    private var movieGenreList: Result.Success<List<MovieGenre>>? = null

    private var movieGenreMap: Result.Success<IntObjectMap<String>>? = null

    override suspend fun getMovieGenreList(): Result<List<MovieGenre>, MovieDataException> =
        genreMutex.withLock { loadGenreList() }

    override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, MovieDataException> =
        genreMutex.withLock {
            movieGenreMap ?: when (val result = loadGenreList()) {
                is Result.Success<List<MovieGenre>> -> {
                    val data = result.data
                    val map = MutableIntObjectMap<String>(data.size)
                    data.forEach { (id, name) ->
                        map[id] = name
                    }
                    Result.Success<IntObjectMap<String>>(map).also { movieGenreMap = it }
                }
                is Result.Error<MovieDataException> -> result
            }
        }

    /**
     * Must only be called while holding [genreMutex] — kotlinx.coroutines' Mutex is not
     * reentrant, so getMovieGenreMap() would deadlock if it went through the public getter.
     * Failures are not cached, so a later caller retries.
     */
    private suspend fun loadGenreList(): Result<List<MovieGenre>, MovieDataException> =
        movieGenreList ?: fetchMovieGenre().also {
            if (it is Result.Success<List<MovieGenre>>) movieGenreList = it
        }
}