package com.qiaoyuang.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ctrip.flight.mmkv.initialize
import com.qiaoyuang.movie.basicui.Loading

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initialize(application)
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun PreviewHome() {
    Loading()
}
