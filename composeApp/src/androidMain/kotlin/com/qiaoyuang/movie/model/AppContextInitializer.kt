package com.qiaoyuang.movie.model

import android.content.Context
import androidx.startup.Initializer

/**
 * OkHttp's disk cache needs a writable directory, and on Android only a [Context] knows
 * where the app-private cache dir lives. This captures the application Context from a
 * ContentProvider that androidx.startup runs before Application.onCreate, so the shared
 * module can reach it without the app module having to pass anything in.
 */
internal lateinit var appContext: Context

internal class AppContextInitializer : Initializer<Context> {

    override fun create(context: Context): Context =
        context.applicationContext.also { appContext = it }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
