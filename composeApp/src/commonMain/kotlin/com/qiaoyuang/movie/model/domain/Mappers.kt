package com.qiaoyuang.movie.model.domain

import com.qiaoyuang.movie.model.dto.ApiMovieDTO
import com.qiaoyuang.movie.model.dto.ApiMovieResponseDTO
import com.qiaoyuang.movie.model.dto.MovieGenreDTO

internal fun ApiMovieDTO.toDomain() = Movie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage,
    genreIds = genreIds,
)

internal fun ApiMovieResponseDTO.toDomain() = MovieResponse(
    page = page,
    results = results.map { it.toDomain() },
    totalPages = totalPages,
)

internal fun MovieGenreDTO.toDomain() = MovieGenre(id = id, name = name)
