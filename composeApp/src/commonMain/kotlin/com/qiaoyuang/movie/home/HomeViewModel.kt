package com.qiaoyuang.movie.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    val movieState: StateFlow<TopMoviesState>
        field = MutableStateFlow<TopMoviesState>(TopMoviesState.SUCCESS(emptyList(), false))

    // Plain vars: getTopMovies() runs only on viewModelScope's Main dispatcher and has no
    // suspension point between the LOADING guard and emit(LOADING), so calls cannot interleave.
    // Adding a dispatcher back to that launch would reintroduce the race these guard against.
    private var currentPage = 1

    private var pageLimit = Int.MAX_VALUE

    fun getTopMovies() = viewModelScope.launch {
        if (movieState.value is TopMoviesState.LOADING)
            return@launch
        val currentList = movieState.value.data
        if (currentPage > pageLimit) {
            movieState.emit(TopMoviesState.SUCCESS(currentList, true))
            return@launch
        }
        movieState.emit(TopMoviesState.LOADING(currentList))
        val state = when (val result = repository.fetchTopRated(currentPage)) {
            is Result.Success<MovieResponse> -> {
                val newList = with(result.data) {
                    currentPage = page + 1
                    pageLimit = totalPages
                    currentList + results
                }
                TopMoviesState.SUCCESS(newList, false)
            }
            is Result.Error<String> -> TopMoviesState.ERROR(currentList)
        }
        movieState.emit(state)
    }

    sealed interface TopMoviesState {

        val data: List<Movie>

        data class LOADING(override val data: List<Movie>) : TopMoviesState

        data class SUCCESS(override val data: List<Movie>, val isNoMore: Boolean) : TopMoviesState

        data class ERROR(override val data: List<Movie>) : TopMoviesState
    }
}