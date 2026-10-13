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
 * Joins a movie's similar movies with the genre catalogue so each row can show its genre names.
 *
 * A class, not an interface plus an Impl: the architecture guide defines use cases as classes
 * and keeps interfaces for the data layer, where faking a source is the point. Nothing ever
 * substituted this one — every test built the real class over a fake repository.
 *
 * Stateless, so the movie is an argument. It was a constructor parameter only because the old
 * interface fixed invoke() as parameterless, which also forced a per-movie Koin factory.
 *
 * No cache of its own: both calls are cache-first in the repository, where the guide puts that
 * decision. What is left is the join, which is the reason this use case exists.
 */
internal class SimilarMovieUseCase(
    private val repository: MovieRepository,
    private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(movieId: Long): Result<List<SimilarMovieShowModel>?, MovieDataException> =
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
