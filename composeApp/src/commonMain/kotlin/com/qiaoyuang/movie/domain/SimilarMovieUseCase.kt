package com.qiaoyuang.movie.domain

import com.qiaoyuang.movie.model.Result
import com.qiaoyuang.movie.model.MovieDataException

internal interface SimilarMovieUseCase {

    suspend operator fun invoke(): Result<List<SimilarMovieShowModel>?, MovieDataException>
}