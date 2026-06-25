package com.qiaoyuang.movie.domain

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.SimilarMovieShowModel
import com.qiaoyuang.movie.model.convertToSimilarMovieShowModel
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class SimilarMovieUseCaseImpl(
    private val repository: MovieRepository,
    private val defaultDispatcher: CoroutineDispatcher,
    private val movieId: Long,
) : SimilarMovieUseCase {

    private val mutex = Mutex()

    private var cache: Result<List<SimilarMovieShowModel>?, String>? = null

    override suspend operator fun invoke(): Result<List<SimilarMovieShowModel>?, String> {
        mutex.withLock {
            cache?.let {
                return it
            }
        }
        return coroutineScope {
            val similarMovieDeferred = async { repository.similarMovies(movieId) }
            val genreMapDeferred = async { repository.getMovieGenreMap() }
            val similarMovieResult = similarMovieDeferred.await()
            val genreMapResult = genreMapDeferred.await()
            if (similarMovieResult is Result.Success<MovieResponse>
                && genreMapResult is Result.Success<IntObjectMap<String>>
            ) withContext(defaultDispatcher) {
                val list = similarMovieResult
                    .data
                    .results
                    .asSequence()
                    .filter { it.posterPath != null }
                    .map { it.convertToSimilarMovieShowModel(genreMapResult.data) }
                    .toList()
                    .takeIf { it.isNotEmpty() }
                val result = Result.Success(list)
                mutex.withLock {
                    cache = result
                }
                result
            } else {
                (similarMovieResult as? Result.Error<String>)
                    ?: (genreMapResult as Result.Error<String>)
            }
        }
    }
}