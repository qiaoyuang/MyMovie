package com.qiaoyuang.movie

import android.content.Context
import com.qiaoyuang.movie.model.initKeyValueStorage
import com.qiaoyuang.movie.model.initKoin

/**
 * Everything the app needs before any UI or other entry point runs. Called once from
 * MovieApplication.onCreate(), which runs on every process start — including one the system
 * spins up purely to serve an AppFunction call, where no Activity is ever created.
 *
 * Storage comes before Koin because definitions in the graph may come to depend on it.
 */
fun setupApp(context: Context) {
    initKeyValueStorage(context)
    initKoin()
}
