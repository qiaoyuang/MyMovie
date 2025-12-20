package com.qiaoyuang.movie

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
internal object Homepage : NavKey

@JvmInline
@Serializable
internal value class DetailedPage(val movieId: Long) : NavKey

@Serializable
internal object SearchPage : NavKey

@JvmInline
@Serializable
internal value class SimilarMoviesPage(val movieId: Long) : NavKey