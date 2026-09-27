package com.qiaoyuang.movie.model.local

/**
 * Which cached list a row belongs to. One string column instead of a table per screen, so one
 * PagingSource and one RemoteMediator serve every paged list.
 */
internal const val TOP_RATED_LIST_KEY = "top_rated"

internal fun similarListKey(movieId: Long): String = "similar:$movieId"
