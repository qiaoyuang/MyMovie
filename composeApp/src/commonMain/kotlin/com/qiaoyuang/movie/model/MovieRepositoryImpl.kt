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

    override suspend infix fun fetchTopRated(page: Int): Result<MovieResponse, String> =
        wrap { service.fetchTopRated(page).toDomain() }

    override suspend fun movieDetail(movieId: Long): Result<Movie, String> =
        wrap { (service movieDetail movieId).toDomain() }

    override suspend fun similarMovies(movieId: Long, page: Int): Result<MovieResponse, String> =
        wrap { service.similarMovies(movieId, page).toDomain() }

    override suspend fun fetchMovieGenre(): Result<List<MovieGenre>, String> =
        wrap { service.fetchMovieGenre().genres.map { it.toDomain() } }

    override suspend fun search(word: String, page: Int): Result<MovieResponse, String> =
        wrap { service.search(word, page).toDomain() }

    private suspend inline fun <T> wrap(crossinline fetch: suspend () -> T): Result<T, String> =
        withContext(defaultDispatcher) {
            try {
                Result.Success(fetch())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(e.message ?: "")
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

    override suspend fun getMovieGenreList(): Result<List<MovieGenre>, String> =
        genreMutex.withLock { loadGenreList() }

    override suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, String> =
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
                is Result.Error<String> -> result
            }
        }

    /**
     * Must only be called while holding [genreMutex] — kotlinx.coroutines' Mutex is not
     * reentrant, so getMovieGenreMap() would deadlock if it went through the public getter.
     * Failures are not cached, so a later caller retries.
     */
    private suspend fun loadGenreList(): Result<List<MovieGenre>, String> =
        movieGenreList ?: fetchMovieGenre().also {
            if (it is Result.Success<List<MovieGenre>>) movieGenreList = it
        }
}