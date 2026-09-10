package com.qiaoyuang.movie

import androidx.compose.ui.window.ComposeUIViewController
import com.ctrip.flight.mmkv.initialize
import com.qiaoyuang.movie.model.initKoin
import platform.UIKit.UIViewController

/** Called from iOSApp.init(), the counterpart to Android's Application.onCreate(). */
fun setupKoin() = initKoin()

fun MainViewController(): UIViewController {
    initialize()
    return ComposeUIViewController { App() }
}