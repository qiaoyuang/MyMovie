package com.qiaoyuang.movie

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
internal object Homepage

@JvmInline
@Serializable
internal value class DetailedPage(val movieId: Long)

@Serializable
internal object SearchPage

@JvmInline
@Serializable
internal value class SimilarMoviesPage(val movieId: Long)