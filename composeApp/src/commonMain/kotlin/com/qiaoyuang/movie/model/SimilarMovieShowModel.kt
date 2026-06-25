package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap
import com.qiaoyuang.movie.model.domain.Movie

internal data class SimilarMovieShowModel(
    val id: Long,
    val title: String,
    val genres: String,
    val posterPath: String?,
    val voteAverage: String?
)

/**
 * Convert ApiFilm to SimilarMovieShowModel
 * @receiver: An ApiFilm object that will be converted
 * @param genres: A genre map (key: index, value: genre name)
 * @return: A SimilarMovieShowModel object from ApiFilm
 */
internal infix fun Movie.convertToSimilarMovieShowModel(genres: IntObjectMap<String>): SimilarMovieShowModel {
    val genresStr = buildString {
        genreIds?.run {
            forEachIndexed { i, key ->
                genres[key]?.let { genreStr ->
                    append(genreStr)
                    if (i < lastIndex) {
                        append(" • ")
                    }
                }
            }
        }
    }
    return SimilarMovieShowModel(
        id = id,
        title = title,
        genres = genresStr,
        posterPath = posterPath,
        voteAverage = voteAverage
    )
}
