package com.qiaoyuang.movie.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.qiaoyuang.movie.model.MOVIE_PAGE_SIZE
import com.qiaoyuang.movie.model.MoviePagingSource
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.domain.Movie
import kotlinx.coroutines.flow.Flow

internal class SimilarMoviesViewModel(
    private val repository: MovieRepository,
    private val movieId: Long,
) : ViewModel() {

    /**
     * The "no more results" and "load failed" toasts this used to push through a
     * SharedFlow<UIEvent> now come from LazyPagingItems.loadState.append in the UI. They were
     * never really one-off events — they were a projection of load state, which is why the
     * ViewModel had to mirror Paging's bookkeeping by hand to produce them.
     */
    val movies: Flow<PagingData<Movie>> = Pager(
        config = PagingConfig(
            pageSize = MOVIE_PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { MoviePagingSource { page -> repository.similarMovies(movieId, page) } },
    ).flow.cachedIn(viewModelScope)
}
