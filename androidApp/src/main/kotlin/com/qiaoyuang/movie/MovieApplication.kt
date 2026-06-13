package com.qiaoyuang.movie

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import com.qiaoyuang.movie.appfunctions.MovieFunctions

class MovieApplication : Application(), AppFunctionConfiguration.Provider {

    override val appFunctionConfiguration: AppFunctionConfiguration =
        AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(MovieFunctions::class.java) { MovieFunctions.getInstance() }
            .build()
}
