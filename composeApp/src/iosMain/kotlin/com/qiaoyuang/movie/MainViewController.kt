package com.qiaoyuang.movie

import androidx.compose.ui.window.ComposeUIViewController
import com.ctrip.flight.mmkv.initialize
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initialize()
    return ComposeUIViewController { App() }
}