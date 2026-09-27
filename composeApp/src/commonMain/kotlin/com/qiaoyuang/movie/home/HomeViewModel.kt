package com.qiaoyuang.movie.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.qiaoyuang.movie.model.MOVIE_PAGE_SIZE
import com.qiaoyuang.movie.model.MovieListPagingSource
import com.qiaoyuang.movie.model.MovieRemoteMediator
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.local.MovieLocalDataSource
import com.qiaoyuang.movie.model.local.TOP_RATED_LIST_KEY
import kotlinx.coroutines.flow.Flow

internal class HomeViewModel(
    repository: MovieRepository,
    private val local: MovieLocalDataSource,
) : ViewModel() {

    /**
     * Offline-first: the PagingSource reads the cache and the mediator writes it, so a warm
     * cache renders before any request goes out and still renders with no network at all.
     *
     * cachedIn(viewModelScope) keeps the loaded pages alive across recompositions and
     * configuration changes; without it every new collector would restart from the top.
     */
    @OptIn(ExperimentalPagingApi::class)
    val movies: Flow<PagingData<Movie>> = Pager(
        config = PagingConfig(
            pageSize = MOVIE_PAGE_SIZE,
            // Paging's defaults are kept: the first load covers three pages, which is also the
            // window restored after each mediator write invalidates the source. Both
            // initialLoadSize and pageSize must stay multiples of MOVIE_PAGE_SIZE so the
            // offsets MovieListPagingSource hands out stay on one grid.
            //
            // No total count is known up front, so there is nothing to show placeholders for.
            enablePlaceholders = false,
        ),
        remoteMediator = MovieRemoteMediator(
            listKey = TOP_RATED_LIST_KEY,
            local = local,
            fetchPage = { page -> repository.fetchTopRated(page) },
        ),
        pagingSourceFactory = { MovieListPagingSource(local, TOP_RATED_LIST_KEY, viewModelScope) },
    ).flow.cachedIn(viewModelScope)
}
