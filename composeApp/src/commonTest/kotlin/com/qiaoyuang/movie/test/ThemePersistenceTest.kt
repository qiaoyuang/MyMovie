package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.basicui.MovieTheme
import com.qiaoyuang.movie.model.SettingsStore
import com.qiaoyuang.movie.model.domain.ThemeMode
import com.qiaoyuang.movie.model.themeModeFrom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MmkvSettingsStore itself cannot run here — MMKV needs its native library and a real Context —
 * so the store is faked and the logic above it is what gets tested: the stored-name mapping, and
 * that MovieTheme both restores from and writes back to whatever it is bound to.
 */
class ThemePersistenceTest {

    private class FakeSettingsStore(private var mode: ThemeMode = ThemeMode.FOLLOW_SYSTEM) : SettingsStore {
        var writes = 0
            private set

        override fun themeMode(): ThemeMode = mode

        override fun setThemeMode(mode: ThemeMode) {
            this.mode = mode
            writes++
        }
    }

    @Test
    fun everyModeSurvivesBeingStoredByName() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, themeModeFrom(mode.name))
        }
    }

    /** A key that was never written reads back as an empty string, not as null. */
    @Test
    fun anAbsentValueFallsBackToFollowingTheSystem() {
        assertEquals(ThemeMode.FOLLOW_SYSTEM, themeModeFrom(""))
        assertEquals(ThemeMode.FOLLOW_SYSTEM, themeModeFrom(null))
    }

    /** A value written by a version that had a mode this one does not must not throw. */
    @Test
    fun anUnrecognisedValueFallsBackToFollowingTheSystem() {
        assertEquals(ThemeMode.FOLLOW_SYSTEM, themeModeFrom("SEPIA"))
        assertEquals(ThemeMode.FOLLOW_SYSTEM, themeModeFrom("light"))
    }

    @Test
    fun bindingLoadsTheSavedChoice() {
        MovieTheme.bind(FakeSettingsStore(ThemeMode.DARK))
        assertEquals(ThemeMode.DARK, MovieTheme.themeMode)

        MovieTheme.bind(FakeSettingsStore(ThemeMode.LIGHT))
        assertEquals(ThemeMode.LIGHT, MovieTheme.themeMode)
    }

    @Test
    fun changingTheThemeWritesItBack() {
        val store = FakeSettingsStore(ThemeMode.FOLLOW_SYSTEM)
        MovieTheme.bind(store)

        MovieTheme.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, MovieTheme.themeMode)
        assertEquals(1, store.writes)
        // What a relaunch would read.
        assertEquals(ThemeMode.DARK, store.themeMode())
    }

    @Test
    fun aRelaunchRestoresWhatTheUserPicked() {
        val store = FakeSettingsStore(ThemeMode.FOLLOW_SYSTEM)
        MovieTheme.bind(store)
        MovieTheme.setThemeMode(ThemeMode.LIGHT)

        // Stand in for a fresh process: a new holder state, the same stored value.
        MovieTheme.bind(store)

        assertEquals(ThemeMode.LIGHT, MovieTheme.themeMode)
    }
}
