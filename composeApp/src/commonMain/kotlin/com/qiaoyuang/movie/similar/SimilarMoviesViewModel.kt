package com.qiaoyuang.movie.similar

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

internal class SimilarMoviesViewModel(
    private val movieId: Long,
    private val repository: MovieRepository,
) : ViewModel() {

    init {
        getSimilarMovies(false)
    }

    private val _movieState = MutableStateFlow<SimilarMoviesState>(SimilarMoviesState.LOADING)
    val movieState: StateFlow<SimilarMoviesState> = _movieState

    @Volatile
    private var currentPage = 1

    @Volatile
    private var pageLimit = Int.MAX_VALUE

    @Volatile
    var isLoading = false
    private var movieList = emptyList<ApiMovie>()

    fun getSimilarMovies(isLoadMore: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            if (isLoading)
                return@launch
            if (currentPage >= pageLimit) {
                SimilarMoviesState.SUCCESS(movieList, LoadingMoreState.NO_MORE)
                return@launch
            }
            isLoading = true
            if (_movieState.value is SimilarMoviesState.ERROR)
                _movieState.emit(SimilarMoviesState.LOADING)
            val state = try {
                with(repository.similarMovies(movieId, currentPage)) {
                    currentPage = page + 1
                    pageLimit = totalPages
                    movieList += results
                }
                SimilarMoviesState.SUCCESS(movieList, LoadingMoreState.SUCCESS)
            } catch (e: Exception) {
                e.printStackTrace()
                if (isLoadMore)
                    SimilarMoviesState.SUCCESS(movieList, LoadingMoreState.FAIL)
                else
                    SimilarMoviesState.ERROR
            }
            _movieState.emit(state)
            isLoading = false
        }
    }

    sealed interface SimilarMoviesState {
        data object LOADING : SimilarMoviesState
        data class SUCCESS(val value: List<ApiMovie>, val loadingMoreState: LoadingMoreState) : SimilarMoviesState
        data object ERROR : SimilarMoviesState
    }
}