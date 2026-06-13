package com.qiaoyuang.movie.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * A movie returned by search, top-rated, or similar-movie queries.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class MovieInfo(
    /** Unique TMDB movie identifier. Pass to getMovieDetails or getSimilarMovies for further lookups. */
    val id: Long,
    /** Full movie title */
    val title: String,
    /** Brief plot overview */
    val overview: String,
    /** Average audience rating out of 10, or null if unavailable */
    val rating: String?,
    /** Full poster image URL, or null if unavailable */
    val posterUrl: String?,
)
