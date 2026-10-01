package com.qiaoyuang.movie.similar

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
import com.qiaoyuang.movie.model.local.similarListKey
import kotlinx.coroutines.flow.Flow

internal class SimilarMoviesViewModel(
    repository: MovieRepository,
    local: MovieLocalDataSource,
    movieId: Long,
) : ViewModel() {

    /**
     * Offline-first, exactly as on the home screen — the mediator and the DB-backed source are
     * generic over the endpoint, so this differs only in the list key and the call it makes.
     *
     * The key is per movie, which is also what lets the detail screen's strip and this screen
     * share one cached list: whichever opens first fills it, and the other finds it there.
     *
     * The "no more results" and "load failed" toasts this used to push through a
     * SharedFlow<UIEvent> now come from LazyPagingItems.loadState.append in the UI. They were
     * never really one-off events — they were a projection of load state, which is why the
     * ViewModel had to mirror Paging's bookkeeping by hand to produce them.
     */
    @OptIn(ExperimentalPagingApi::class)
    val movies: Flow<PagingData<Movie>> = similarListKey(movieId).let { listKey ->
        Pager(
            config = PagingConfig(pageSize = MOVIE_PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = MovieRemoteMediator(
                listKey = listKey,
                local = local,
                fetchPage = { page -> repository.similarMovies(movieId, page) },
            ),
            pagingSourceFactory = { MovieListPagingSource(local, listKey, viewModelScope) },
        ).flow.cachedIn(viewModelScope)
    }
}
