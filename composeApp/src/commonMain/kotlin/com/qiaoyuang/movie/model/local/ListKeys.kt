package com.qiaoyuang.movie.model.local

/**
 * Which cached list a row belongs to. One string column instead of a table per screen, so one
 * PagingSource and one RemoteMediator serve every paged list.
 */
internal const val TOP_RATED_LIST_KEY = "top_rated"

internal fun similarListKey(movieId: Long): String = "similar:$movieId"

/**
 * The genre catalogue is not paged, but it is cached and it expires, so it gets a key of its own
 * in the cursor table purely for the refresh stamp — one page, fully fetched, no next page. That
 * lets it share the same TTL as every other cached list instead of inventing a second mechanism.
 */
internal const val GENRES_LIST_KEY = "genres"
