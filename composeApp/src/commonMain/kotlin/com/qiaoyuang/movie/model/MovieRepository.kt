package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import androidx.paging.PagingData
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow

@Mockable
internal interface MovieRepository {

    suspend infix fun fetchTopRated(page: Int = 1): Result<MovieResponse, MovieDataException>

    suspend fun movieDetail(movieId: Long): Result<Movie, MovieDataException>

    /** Always a request. MovieRemoteMediator is the thing that fills the cache, so it cannot read it. */
    suspend fun similarMovies(movieId: Long, page: Int = 1): Result<MovieResponse, MovieDataException>

    /**
     * The first page of similar movies, cache first — what the detail screen's strip shows.
     * Shares the cached list the paged "all similar movies" screen pages through, so whichever
     * of the two opens first fills it for the other.
     */
    suspend fun similarMoviesFirstPage(movieId: Long): Result<List<Movie>, MovieDataException>

    /**
     * The top-rated list, offline-first: paged out of the cache, which a RemoteMediator refills
     * from the network. Not cached here — the ViewModel applies cachedIn, which is where the
     * loaded pages' lifetime belongs.
     */
    fun pagedTopRatedMovies(): Flow<PagingData<Movie>>

    /** The same for one movie's similar movies, on the list the detail strip also reads. */
    fun pagedSimilarMovies(movieId: Long): Flow<PagingData<Movie>>

    suspend fun fetchMovieGenre(): Result<List<MovieGenre>, MovieDataException>

    suspend fun search(word: String, page: Int): Result<MovieResponse, MovieDataException>

    suspend fun getMovieGenreList(): Result<List<MovieGenre>, MovieDataException>

    suspend fun getMovieGenreMap(): Result<IntObjectMap<String>, MovieDataException>
}
