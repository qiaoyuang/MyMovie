package com.qiaoyuang.movie.model.local

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** How long a cached list is trusted before the mediator refreshes it on open. */
internal const val CACHE_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000

/**
 * A list is stale once it is older than the TTL — and also if its timestamp lies in the future,
 * which happens when the device clock moves backwards. Without that second case a clock jump
 * would make the cache look fresh for as long as the jump lasted.
 */
internal fun isStale(lastRefreshedAt: Long, now: Long): Boolean =
    (now - lastRefreshedAt) !in 0 until CACHE_TTL_MILLIS

/** Wall clock, not monotonic: the timestamp has to stay meaningful across process death. */
@OptIn(ExperimentalTime::class)
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
