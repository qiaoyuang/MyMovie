package com.qiaoyuang.movie.model.domain

internal data class MovieResponse(
    val page: Int,
    val results: List<Movie>,
    val totalPages: Int,
)
