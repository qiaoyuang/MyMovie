package com.qiaoyuang.movie.domain

import com.qiaoyuang.movie.model.Result

internal interface SimilarMovieUseCase {

    suspend operator fun invoke(): Result<List<SimilarMovieShowModel>?, String>
}