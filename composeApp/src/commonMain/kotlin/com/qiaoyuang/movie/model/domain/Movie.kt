package com.qiaoyuang.movie.model.domain

internal data class Movie(
    val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: String?,
    val genreIds: List<Int>?,
)