package com.qiaoyuang.movie.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val repository: MovieRepository,
    private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    val movieState: StateFlow<TopMoviesState>
        field = MutableStateFlow<TopMoviesState>(TopMoviesState.SUCCESS(emptyList(), false))

    private val currentPage = atomic(1)

    private val pageLimit = atomic(Int.MAX_VALUE)

    fun getTopMovies() = viewModelScope.launch(defaultDispatcher) {
        if (movieState.value is TopMoviesState.LOADING)
            return@launch
        val currentList = movieState.value.data
        if (currentPage.value > pageLimit.value) {
            movieState.emit(TopMoviesState.SUCCESS(currentList, true))
            return@launch
        }
        movieState.emit(TopMoviesState.LOADING(currentList))
        val state = when (val result = repository.fetchTopRated(currentPage.value)) {
            is Result.Success<MovieResponse> -> {
                val newList = with(result.data) {
                    currentPage.value = page + 1
                    pageLimit.value = totalPages
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