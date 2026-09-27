package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.model.local.CACHE_TTL_MILLIS
import com.qiaoyuang.movie.model.local.isStale
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheFreshnessTest {

    private val refreshedAt = 1_700_000_000_000L

    @Test
    fun sevenDaysIsTheBoundary() {
        assertEquals7Days()
        assertFalse(isStale(refreshedAt, refreshedAt))
        assertFalse(isStale(refreshedAt, refreshedAt + CACHE_TTL_MILLIS - 1))
        assertTrue(isStale(refreshedAt, refreshedAt + CACHE_TTL_MILLIS))
    }

    private fun assertEquals7Days() {
        assertTrue(CACHE_TTL_MILLIS == 604_800_000L, "TTL should be 7 days")
    }

    /**
     * A device clock moved backwards would otherwise make the cache look fresh for as long as
     * the jump lasted — up to seven days of never re-fetching.
     */
    @Test
    fun aTimestampInTheFutureCountsAsStale() {
        assertTrue(isStale(refreshedAt, refreshedAt - 1))
        assertTrue(isStale(refreshedAt, 0L))
    }
}
