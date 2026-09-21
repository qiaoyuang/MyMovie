package com.qiaoyuang.movie

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import com.qiaoyuang.movie.appfunctions.MovieFunctions

class MovieApplication : Application(), AppFunctionConfiguration.Provider {

    override fun onCreate() {
        super.onCreate()
        setupApp(this)
    }

    override val appFunctionConfiguration: AppFunctionConfiguration =
        AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(MovieFunctions::class.java) { MovieFunctions.instance }
            .build()
}
