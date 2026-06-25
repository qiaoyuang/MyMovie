package com.qiaoyuang.movie.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.domain.SimilarMovieUseCase
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.SimilarMovieShowModel
import com.qiaoyuang.movie.model.domain.Movie
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class DetailViewModel(
    private val repository: MovieRepository,
    private val similarMovieUseCase: SimilarMovieUseCase,
    private val movieId: Long,
) : ViewModel() {

    /*class Factory(private val movieId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
            DetailViewModel(MovieRepository, movieId) as T
    }*/

    val movieDetailState: StateFlow<MovieDetailState>
        field = MutableStateFlow<MovieDetailState>(MovieDetailState.LOADING)

    fun updateUI() = viewModelScope.launch {
        val (detailResult, similarMovieResult) = supervisorScope {
            val detailDeferred = async { repository.movieDetail(movieId) }
            val similarMovieDeferred = async { similarMovieUseCase() }
            detailDeferred.await() to similarMovieDeferred.await()
        }
        movieDetailState.value = when (detailResult) {
            is Result.Success<Movie> -> MovieDetailState.SUCCESS(
                movie = detailResult.data,
                similarMovies = (similarMovieResult as? Result.Success<List<SimilarMovieShowModel>?>)?.data,
            )
            is Result.Error<String> -> MovieDetailState.ERROR(detailResult.error)
        }
    }

    sealed interface MovieDetailState {
        data object LOADING : MovieDetailState

        data class SUCCESS(
            val movie: Movie,
            val similarMovies: List<SimilarMovieShowModel>?,
        ) : MovieDetailState

        data class ERROR(val message: String) : MovieDetailState
    }
}