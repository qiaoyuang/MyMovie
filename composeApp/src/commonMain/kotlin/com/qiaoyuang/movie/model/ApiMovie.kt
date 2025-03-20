package com.qiaoyuang.movie.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiMovie(
    val id: Long,
    val title: String,
    val overview: String,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("vote_average") val voteAverage: String?,
    @SerialName("genre_ids") val genreIds: List<Int>?,
)

/**
 * API response model for movie genres
 */
@Serializable
data class ApiMovieGenresResponse(val genres: List<MovieGenre>)

/**
 * Movie genre entity
 */
@Serializable
data class MovieGenre(val id: Int, val name: String)

