package com.qiaoyuang.movie.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class SimilarMoviesViewModel(
    private val repository: MovieRepository,
    private val movieId: Long,
) : ViewModel() {

    val movieState: StateFlow<SimilarMoviesState>
        field = MutableStateFlow<SimilarMoviesState>(SimilarMoviesState.SUCCESS(emptyList(), false))

    private val currentPage = atomic(1)

    private val pageLimit = atomic(Int.MAX_VALUE)

    fun getSimilarMovies() = viewModelScope.launch(Dispatchers.Default) {
        if (movieState.value is SimilarMoviesState.LOADING)
            return@launch
        val currentList = movieState.value.data
        if (currentPage.value > pageLimit.value) {
            movieState.emit(SimilarMoviesState.SUCCESS(currentList, true))
            return@launch
        }
        movieState.emit(SimilarMoviesState.LOADING(currentList))

        val state = when (val result = repository.similarMovies(movieId, currentPage.value)) {
            is Result.Success<MovieResponse> -> {
                val list = with(result.data) {
                    currentPage.value = page + 1
                    pageLimit.value = totalPages
                    currentList + results
                }
                SimilarMoviesState.SUCCESS(list, false)
            }
            is Result.Error<String> -> SimilarMoviesState.ERROR(currentList)
        }
        movieState.emit(state)
    }

    sealed interface SimilarMoviesState {

        val data: List<Movie>

        data class LOADING(override val data: List<Movie>) : SimilarMoviesState

        data class SUCCESS(override val data: List<Movie>, val isNoMore: Boolean) : SimilarMoviesState

        data class ERROR(override val data: List<Movie>) : SimilarMoviesState
    }
}