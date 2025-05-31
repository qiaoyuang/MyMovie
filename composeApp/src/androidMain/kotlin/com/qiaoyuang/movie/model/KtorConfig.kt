package com.qiaoyuang.movie.model

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual val ktorEngine: HttpClientEngine = OkHttp.create()