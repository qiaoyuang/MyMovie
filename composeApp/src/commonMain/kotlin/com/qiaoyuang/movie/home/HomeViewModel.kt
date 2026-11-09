package com.qiaoyuang.movie.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.qiaoyuang.movie.model.MovieRepository
import com.qiaoyuang.movie.model.domain.Movie
import kotlinx.coroutines.flow.Flow

internal class HomeViewModel(repository: MovieRepository) : ViewModel() {

    /**
     * Offline-first, which the repository takes care of: a warm cache renders before any request
     * goes out, and still renders with no network at all.
     *
     * cachedIn(viewModelScope) keeps the loaded pages alive across recompositions and
     * configuration changes; without it every new collector would restart from the top.
     */
    val movies: Flow<PagingData<Movie>> = repository.pagedTopRatedMovies().cachedIn(viewModelScope)
}
