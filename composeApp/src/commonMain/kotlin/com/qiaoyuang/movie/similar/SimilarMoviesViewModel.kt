package com.qiaoyuang.movie.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieResponse
import com.qiaoyuang.movie.model.ui.UIEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class SimilarMoviesViewModel(
    private val repository: MovieRepository,
    private val movieId: Long,
) : ViewModel() {

    val movieState: StateFlow<SimilarMoviesState>
        field = MutableStateFlow(
            value = SimilarMoviesState(
                data = emptyList(),
                isNoMore = false,
                isLoading = false,
                isError = false,
            )
        )

    val uiEventFlow: SharedFlow<UIEvent>
        field = MutableSharedFlow()

    private var currentPage = 1
    private var pageLimit = Int.MAX_VALUE

    fun getSimilarMovies() = viewModelScope.launch {
        if (movieState.value.isLoading)
            return@launch
        val oldState = movieState.value
        if (currentPage > pageLimit) {
            movieState.value = oldState.copy(isNoMore = true)
            uiEventFlow.emit(UIEvent.CommonNoMoreToast)
            return@launch
        }
        movieState.value = oldState.copy(isLoading = true, isError = false)

        when (val result = repository.similarMovies(movieId, currentPage)) {
            is Result.Success<MovieResponse> -> {
                val currentList = oldState.data
                val list = with(result.data) {
                    currentPage = page + 1
                    pageLimit = totalPages
                    currentList + results
                }
                movieState.value = oldState.copy(
                    data = list,
                    isLoading = false,
                    isError = false,
                )
            }
            is Result.Error<String> -> {
                movieState.value = oldState.copy(isLoading = false, isError = true)
                uiEventFlow.emit(UIEvent.CommonErrorToast)
            }
        }
    }

    data class SimilarMoviesState(
        val data: List<Movie>,
        val isNoMore: Boolean,
        val isLoading: Boolean,
        val isError: Boolean,
    )
}