package com.qiaoyuang.movie.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiaoyuang.movie.basicui.LoadingMoreState
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

internal class HomeViewModel(private val repository: MovieRepository) : ViewModel() {

    init {
        getTopMovies(false)
    }

    private val _movieState = MutableStateFlow<TopMoviesState>(TopMoviesState.LOADING)
    val movieState: StateFlow<TopMoviesState> = _movieState

    @Volatile
    private var currentPage = 1

    @Volatile
    private var pageLimit = Int.MAX_VALUE

    @Volatile
    var isLoading = false
    private var movieList = emptyList<ApiMovie>()

    fun getTopMovies(isLoadMore: Boolean) = viewModelScope.launch(Dispatchers.Default) {
        if (isLoading || currentPage >= pageLimit)
            return@launch
        if (currentPage >= pageLimit) {
            TopMoviesState.SHOW(movieList, LoadingMoreState.NO_MORE)
            return@launch
        }
        isLoading = true
        if (_movieState.value is TopMoviesState.ERROR)
            _movieState.emit(TopMoviesState.LOADING)
        val state = try {
            with(repository.fetchTopRated(currentPage)) {
                currentPage = page + 1
                pageLimit = totalPages
                movieList += results
            }
            TopMoviesState.SHOW(movieList, LoadingMoreState.SUCCESS)
        } catch (e: Exception) {
            e.printStackTrace()
            if (isLoadMore)
                TopMoviesState.SHOW(movieList, LoadingMoreState.FAIL)
            else
                TopMoviesState.ERROR
        }
        _movieState.emit(state)
        isLoading = false
    }

    sealed interface TopMoviesState {
        data object LOADING : TopMoviesState
        data class SHOW(val value: List<ApiMovie>, val loadingMoreState: LoadingMoreState) : TopMoviesState
        data object ERROR : TopMoviesState
    }
}