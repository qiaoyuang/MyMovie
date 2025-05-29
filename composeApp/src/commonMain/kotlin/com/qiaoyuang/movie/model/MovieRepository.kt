package com.qiaoyuang.movie.model

import androidx.collection.IntObjectMap

interface MovieRepository : APIService {

    suspend fun getMovieGenreList(): List<MovieGenre>

    suspend fun getMovieGenreMap(): IntObjectMap<String>
}