package com.qiaoyuang.movie.similar

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

internal class SimilarMoviesViewModel(
    private val repository: MovieRepository,
    private val movieId: Long,
) : ViewModel() {

    private val _movieState = MutableStateFlow<SimilarMoviesState>(SimilarMoviesState.SUCCESS(emptyList(), false))
    val movieState: StateFlow<SimilarMoviesState> = _movieState

    @Volatile
    private var currentPage = 1

    @Volatile
    private var pageLimit = Int.MAX_VALUE

    fun getSimilarMovies() = viewModelScope.launch(Dispatchers.Default) {
        if (movieState.value is SimilarMoviesState.LOADING)
            return@launch
        val currentList = movieState.value.data
        if (currentPage > pageLimit) {
            _movieState.emit(SimilarMoviesState.SUCCESS(currentList, true))
            return@launch
        }
        _movieState.emit(SimilarMoviesState.LOADING(currentList))
        val state = try {
            val list = with(repository.similarMovies(movieId, currentPage)) {
                currentPage = page + 1
                pageLimit = totalPages
                currentList + results
            }
            SimilarMoviesState.SUCCESS(list, false)
        } catch (e: Exception) {
            e.printStackTrace()
            SimilarMoviesState.ERROR(currentList)
        }
        _movieState.emit(state)
    }

    sealed interface SimilarMoviesState {

        val data: List<ApiMovie>

        @JvmInline
        value class LOADING(override val data: List<ApiMovie>) : SimilarMoviesState

        data class SUCCESS(override val data: List<ApiMovie>, val isNoMore: Boolean) : SimilarMoviesState

        @JvmInline
        value class ERROR(override val data: List<ApiMovie>) : SimilarMoviesState
    }
}