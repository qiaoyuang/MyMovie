package com.qiaoyuang.movie.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.SimilarMovieShowModel
import com.qiaoyuang.movie.model.convertToSimilarMovieShowModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class DetailViewModel(
    private val repository: MovieRepository,
    private val movieId: Long,
) : ViewModel() {

    init {
        updateUI()
    }

    /*class Factory(private val movieId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
            DetailViewModel(MovieRepository, movieId) as T
    }*/

    private val _movieDetailState = MutableStateFlow<MovieDetailState>(MovieDetailState.LOADING)
    val movieDetailState: StateFlow<MovieDetailState> = _movieDetailState

    private suspend fun fetchMovieDetail(): ApiMovie? = try {
        repository.movieDetail(movieId)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private suspend fun fetchSimilarMovieDetail(): List<SimilarMovieShowModel>? = try {
        val deferred = viewModelScope.async {
            repository
                .similarMovies(movieId)
                .results
                .asSequence()
                .filter { it.posterPath != null }
        }
        val genres = repository.getMovieGenres()
        deferred.await()
            .map { it.convertToSimilarMovieShowModel(genres) }
            .toList()
            .takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun updateUI() = viewModelScope.launch(Dispatchers.Default) {
        if (_movieDetailState.value == MovieDetailState.ERROR)
            _movieDetailState.emit(MovieDetailState.LOADING)
        val movieDetailDeferred = async { fetchMovieDetail() }
        val similarMovies = fetchSimilarMovieDetail()
        val state = movieDetailDeferred.await()?.let {
            MovieDetailState.SUCCESS(it, similarMovies)
        } ?: MovieDetailState.ERROR
        _movieDetailState.emit(state)
    }

    sealed interface MovieDetailState {
        data object LOADING : MovieDetailState

        data class SUCCESS(
            val movie: ApiMovie,
            val similarMovies: List<SimilarMovieShowModel>?,
        ) : MovieDetailState

        data object ERROR : MovieDetailState
    }
}