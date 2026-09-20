package com.qiaoyuang.movie.home

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

internal class HomeViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    /**
     * Everything the old hand-rolled version tracked — current page, page limit, accumulated
     * list, loading/error/no-more flags, re-entrancy guard — now lives inside Paging.
     *
     * cachedIn(viewModelScope) keeps the loaded pages alive across recompositions and
     * configuration changes; without it every new collector would restart from page one.
     */
    val movies: Flow<PagingData<Movie>> = Pager(
        config = PagingConfig(
            pageSize = MOVIE_PAGE_SIZE,
            // No total count is known up front, so there is nothing to show placeholders for.
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { MoviePagingSource { page -> repository.fetchTopRated(page) } },
    ).flow.cachedIn(viewModelScope)
}
