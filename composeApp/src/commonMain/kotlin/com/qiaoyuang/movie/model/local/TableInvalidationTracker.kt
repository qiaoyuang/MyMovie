package com.qiaoyuang.movie.model.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update

/**
 * sqllin has no change notification of its own — nothing like Room's InvalidationTracker — so
 * writers announce themselves here and DB-backed PagingSources listen. Every write goes through
 * MovieLocalDataSource, which is the only holder of the Database, so this is the complete set of
 * events.
 *
 * Versions rather than a bare signal, because a PagingSource is created before it can start
 * collecting: it records the version it was built against and [invalidationOf] fires
 * immediately if a write already moved past it. A plain SharedFlow would drop that write and
 * leave the source serving stale rows forever.
 *
 * Tracked per listKey so refreshing "top_rated" does not invalidate "similar:550".
 */
internal class TableInvalidationTracker {

    private val versions = MutableStateFlow<Map<String, Long>>(emptyMap())

    fun versionOf(listKey: String): Long = versions.value[listKey] ?: INITIAL_VERSION

    /** MutableStateFlow.update is a CAS loop, so concurrent writers cannot lose an increment. */
    fun notifyChanged(listKey: String) {
        versions.update { current ->
            current + (listKey to ((current[listKey] ?: INITIAL_VERSION) + 1))
        }
    }

    /**
     * Completes once [listKey] has changed since [since]. Collect it to learn that a
     * PagingSource built at [since] is now stale.
     */
    fun invalidationOf(listKey: String, since: Long): Flow<Unit> =
        versions
            .map { it[listKey] ?: INITIAL_VERSION }
            .filter { it > since }
            .take(1)
            .map { }

    private companion object {
        const val INITIAL_VERSION = 0L
    }
}
