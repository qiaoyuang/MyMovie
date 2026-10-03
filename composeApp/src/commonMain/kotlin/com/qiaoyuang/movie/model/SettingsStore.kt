package com.qiaoyuang.movie.model

import com.ctrip.flight.mmkv.MMKV_KMP
import com.ctrip.flight.mmkv.defaultMMKV
import com.qiaoyuang.movie.model.domain.ThemeMode

/**
 * The app's own preferences. Deliberately not in the database: a single enum with no relational
 * shape, read once before the first frame and written on a tap.
 *
 * Synchronous, unlike the rest of the data layer. MMKV is a memory-mapped file, so a read or
 * write is a memcpy rather than I/O — which is the reason this project uses it over DataStore.
 * Making these suspend would buy no main-thread safety worth having and would stop the saved
 * theme from being in place before composition starts.
 *
 * An interface so the tiny amount of logic above it can be tested: MMKV needs its native library
 * and a real Context, so [MmkvSettingsStore] cannot run in host tests.
 */
internal interface SettingsStore {

    fun themeMode(): ThemeMode

    fun setThemeMode(mode: ThemeMode)
}

/**
 * Stored by name, not by ordinal: an ordinal silently changes meaning if the enum is ever
 * reordered, and the name survives that. An unknown or absent value — a key never written, or
 * one written by a version that had a mode this one does not — falls back to the default rather
 * than throwing.
 */
internal fun themeModeFrom(stored: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.FOLLOW_SYSTEM

internal class MmkvSettingsStore : SettingsStore {

    /**
     * Resolved lazily, so constructing this cannot run before setupApp() has initialised MMKV.
     */
    private val mmkv: MMKV_KMP by lazy { defaultMMKV() }

    override fun themeMode(): ThemeMode = themeModeFrom(mmkv.getString(KEY_THEME_MODE))

    override fun setThemeMode(mode: ThemeMode) {
        mmkv[KEY_THEME_MODE] = mode.name
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
