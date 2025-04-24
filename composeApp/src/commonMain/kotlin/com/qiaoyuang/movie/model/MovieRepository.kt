package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap

interface MovieRepository : APIService {

    suspend fun getMovieGenres(): IntObjectMap<String>
}