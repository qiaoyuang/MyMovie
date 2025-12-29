package com.qiaoyuang.movie.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline

internal class HomeViewModel(private val repository: MovieRepository) : ViewModel() {

    val movieState: StateFlow<TopMoviesState>
        field = MutableStateFlow<TopMoviesState>(TopMoviesState.SUCCESS(emptyList(), false))

    private val currentPage = atomic(1)

    private val pageLimit = atomic(Int.MAX_VALUE)

    fun getTopMovies() = viewModelScope.launch(Dispatchers.Default) {
        if (movieState.value is TopMoviesState.LOADING)
            return@launch
        val currentList = movieState.value.data
        if (currentPage.value > pageLimit.value) {
            movieState.emit(TopMoviesState.SUCCESS(currentList, true))
            return@launch
        }
        movieState.emit(TopMoviesState.LOADING(currentList))
        val state = try {
            val newList = with(repository.fetchTopRated(currentPage.value)) {
                currentPage.value = page + 1
                pageLimit.value = totalPages
                currentList + results
            }
            TopMoviesState.SUCCESS(newList, false)
        } catch (e: Exception) {
            e.printStackTrace()
            TopMoviesState.ERROR(currentList)
        }
        movieState.emit(state)
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