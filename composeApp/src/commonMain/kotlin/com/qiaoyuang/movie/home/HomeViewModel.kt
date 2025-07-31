package com.qiaoyuang.movie.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.jvm.JvmInline

internal class HomeViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _movieState = MutableStateFlow<TopMoviesState>(TopMoviesState.SUCCESS(emptyList(), false))
    val movieState: StateFlow<TopMoviesState> = _movieState

    @Volatile
    private var currentPage = 1

    @Volatile
    private var pageLimit = Int.MAX_VALUE

    fun getTopMovies() = viewModelScope.launch(Dispatchers.Default) {
        if (movieState.value is TopMoviesState.LOADING)
            return@launch
        val currentList = movieState.value.data
        if (currentPage > pageLimit) {
            _movieState.emit(TopMoviesState.SUCCESS(currentList, true))
            return@launch
        }
        _movieState.emit(TopMoviesState.LOADING(currentList))
        val state = try {
            val newList = with(repository.fetchTopRated(currentPage)) {
                currentPage = page + 1
                pageLimit = totalPages
                currentList + results
            }
            TopMoviesState.SUCCESS(newList, false)
        } catch (e: Exception) {
            e.printStackTrace()
            TopMoviesState.ERROR(currentList)
        }
        _movieState.emit(state)
    }

    sealed interface TopMoviesState {

        val data: List<ApiMovie>

        @JvmInline
        value class LOADING(override val data: List<ApiMovie>) : TopMoviesState

        data class SUCCESS(override val data: List<ApiMovie>, val isNoMore: Boolean) : TopMoviesState

        @JvmInline
        value class ERROR(override val data: List<ApiMovie>) : TopMoviesState
    }
}