package com.qiaoyuang.movie.model

import android.content.Context
import com.ctrip.flight.mmkv.initialize

/**
 * Initializes whichever key-value storage backs the app. The app entry points call setupApp()
 * rather than this, and nothing outside the data layer references MMKV, so switching the
 * implementation (MMKV-Kotlin or DataStore) only changes this function.
 */
internal fun initKeyValueStorage(context: Context) {
    initialize(context)
}
