package com.qiaoyuang.movie.model

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual val ktorEngine: HttpClientEngine = Darwin.create {
    configureRequest {
        setAllowsCellularAccess(true)
    }
}