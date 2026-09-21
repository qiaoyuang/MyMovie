package com.qiaoyuang.movie

import com.qiaoyuang.movie.model.initKeyValueStorage
import com.qiaoyuang.movie.model.initKoin

/**
 * Everything the app needs at launch, before any UI. Called once from iOSApp.init(), the
 * counterpart to Android's Application.onCreate(); anything else iOS must run at startup
 * belongs here too.
 *
 * Not named initApp: Kotlin/Native exports functions starting with "init" to Objective-C with
 * a "do" prefix, so Swift would have to call doInitApp().
 *
 * Storage comes before Koin because definitions in the graph may come to depend on it.
 */
fun setupApp() {
    initKeyValueStorage()
    initKoin()
}
