package com.qiaoyuang.movie.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.domain.Movie
import kotlinx.coroutines.flow.Flow

internal class SimilarMoviesViewModel(repository: MovieRepository, movieId: Long) : ViewModel() {

    /**
     * Offline-first, exactly as on the home screen, and on the same cached list the detail
     * screen's strip reads — whichever opens first fills it for the other.
     */
    val movies: Flow<PagingData<Movie>> = repository.pagedSimilarMovies(movieId).cachedIn(viewModelScope)
}
