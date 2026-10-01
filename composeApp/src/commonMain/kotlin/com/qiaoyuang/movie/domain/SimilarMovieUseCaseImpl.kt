package com.qiaoyuang.movie.domain

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.model.MovieDataException
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Joins the similar movies with the genre catalogue so each row can show its genre names.
 *
 * It no longer keeps a cache of its own. Both of the calls below are cache-first in the
 * repository now, which is where the architecture guide puts that decision — a memo here would
 * be a third tier shadowing it, and it would have gone stale independently of the table it was
 * derived from. What is left is the join, which is the only reason this use case exists.
 */
internal class SimilarMovieUseCaseImpl(
    private val repository: MovieRepository,
    private val defaultDispatcher: CoroutineDispatcher,
    private val movieId: Long,
) : SimilarMovieUseCase {

    override suspend operator fun invoke(): Result<List<SimilarMovieShowModel>?, MovieDataException> =
        coroutineScope {
            val similarMoviesDeferred = async { repository.similarMoviesFirstPage(movieId) }
            val genreMapDeferred = async { repository.getMovieGenreMap() }
            val similarMoviesResult = similarMoviesDeferred.await()
            val genreMapResult = genreMapDeferred.await()
            if (similarMoviesResult is Result.Success<List<Movie>>
                && genreMapResult is Result.Success<IntObjectMap<String>>
            ) withContext(defaultDispatcher) {
                Result.Success(
                    similarMoviesResult
                        .data
                        .asSequence()
                        // A row without a poster would render as an empty card.
                        .filter { it.posterPath != null }
                        .map { it convertToSimilarMovieShowModel genreMapResult.data }
                        .toList()
                        .takeIf { it.isNotEmpty() }
                )
            } else {
                (similarMoviesResult as? Result.Error<MovieDataException>)
                    ?: (genreMapResult as Result.Error<MovieDataException>)
            }
        }
}
