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

internal class SimilarMoviesViewModel(
    private val movieId: Long,
    private val repository: MovieRepository,
) : ViewModel() {

    companion object {
        const val PAGE_LIMIT = 500
    }

    init {
        getSimilarMovies(false)
    }

    private val _movieState = MutableStateFlow<SimilarMoviesState>(SimilarMoviesState.LOADING)
    val movieState: StateFlow<SimilarMoviesState> = _movieState

    @Volatile
    private var page = 1
    @Volatile
    var isLoading = false
    private var movieList = emptyList<ApiMovie>()

    fun getSimilarMovies(isLoadMore: Boolean) {
        if (isLoading || page > PAGE_LIMIT)
            return
        isLoading = true
        viewModelScope.launch(Dispatchers.Default) {
            if (_movieState.value is SimilarMoviesState.ERROR)
                _movieState.emit(SimilarMoviesState.LOADING)
            val state = try {
                movieList += repository.similarMovies(movieId, page++).results
                SimilarMoviesState.SUCCESS(movieList, false)
            } catch (e: Exception) {
                e.printStackTrace()
                page--
                if (isLoadMore)
                    SimilarMoviesState.SUCCESS(movieList, true)
                else
                    SimilarMoviesState.ERROR
            }
            _movieState.emit(state)
            isLoading = false
        }
    }

    sealed interface SimilarMoviesState {
        data object LOADING : SimilarMoviesState
        data class SUCCESS(val value: List<ApiMovie>, val isLoadMoreFail: Boolean) : SimilarMoviesState
        data object ERROR : SimilarMoviesState
    }
}