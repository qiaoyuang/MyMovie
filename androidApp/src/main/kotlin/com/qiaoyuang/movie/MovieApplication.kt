package com.qiaoyuang.movie

import android.app.Application

class MovieApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupApp(this)
    }
}
