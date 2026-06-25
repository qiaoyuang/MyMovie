package com.qiaoyuang.movie.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ApiMovieResponseDTO(
    val page: Int,
    val results: List<ApiMovieDTO>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int,
)