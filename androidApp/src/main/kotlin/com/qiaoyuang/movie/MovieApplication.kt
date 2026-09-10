package com.qiaoyuang.movie

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import com.qiaoyuang.movie.appfunctions.MovieFunctions
import com.qiaoyuang.movie.model.initKoin

class MovieApplication : Application(), AppFunctionConfiguration.Provider {

    override fun onCreate() {
        super.onCreate()
        // Runs on every process start, including one the system spins up purely to serve
        // an AppFunction call, where no Activity or composition is ever created.
        initKoin()
    }

    override val appFunctionConfiguration: AppFunctionConfiguration =
        AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(MovieFunctions::class.java) { MovieFunctions.instance }
            .build()
}
