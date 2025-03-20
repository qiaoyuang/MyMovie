package com.qiaoyuang.movie

import kotlinx.serialization.Serializable

@Serializable
object Homepage

@Serializable
data class DetailedPage(val movieId: Long)

@Serializable
object SearchPage