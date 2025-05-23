package com.qiaoyuang.movie

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
object Homepage

@JvmInline
@Serializable
value class DetailedPage(val movieId: Long)

@Serializable
object SearchPage

@JvmInline
@Serializable
value class SimilarMoviesPage(val movieId: Long)