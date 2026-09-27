package com.qiaoyuang.movie.test

import app.cash.turbine.test
import com.qiaoyuang.movie.model.local.TableInvalidationTracker
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TableInvalidationTrackerTest {

    private val topRated = "top_rated"
    private val similar = "similar:550"

    @Test
    fun anUntouchedListStartsAtVersionZero() {
        val tracker = TableInvalidationTracker()
        assertEquals(0L, tracker.versionOf(topRated))
    }

    @Test
    fun eachWriteBumpsOnlyItsOwnList() {
        val tracker = TableInvalidationTracker()
        tracker.notifyChanged(topRated)
        tracker.notifyChanged(topRated)
        assertEquals(2L, tracker.versionOf(topRated))
        assertEquals(0L, tracker.versionOf(similar))
    }

    @Test
    fun aListerWaitsUntilItsOwnListChanges() = runTest {
        val tracker = TableInvalidationTracker()
        tracker.invalidationOf(topRated, since = tracker.versionOf(topRated)).test {
            expectNoEvents()
            tracker.notifyChanged(similar)
            expectNoEvents()
            tracker.notifyChanged(topRated)
            awaitItem()
            awaitComplete()
        }
    }

    /**
     * The reason versions exist. A PagingSource is built, then starts collecting; a write landing
     * in between must not be lost, or the source would serve stale rows forever. Recording the
     * version at construction makes that write already visible when collection starts.
     */
    @Test
    fun aWriteBeforeCollectionStartsStillInvalidates() = runTest {
        val tracker = TableInvalidationTracker()
        val versionAtConstruction = tracker.versionOf(topRated)
        tracker.notifyChanged(topRated)

        tracker.invalidationOf(topRated, since = versionAtConstruction).test {
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun invalidationFiresOnceAndCompletes() = runTest {
        val tracker = TableInvalidationTracker()
        tracker.invalidationOf(topRated, since = 0L).test {
            tracker.notifyChanged(topRated)
            awaitItem()
            tracker.notifyChanged(topRated)
            awaitComplete()
        }
    }
}
