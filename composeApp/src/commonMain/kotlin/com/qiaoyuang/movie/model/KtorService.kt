package com.qiaoyuang.movie.model

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object KtorService : KoinComponent {

    // Will move it to resource
    private const val KEY = "e4f9e61f6ffd66639d33d3dde7e3159b"

    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val API_KEY_PARAM = "api_key"

    val client = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(get())
        }
        defaultRequest {
            url(BASE_URL)
            url.parameters.append(API_KEY_PARAM, KEY)
        }
    }

    infix fun buildImageUrl(path: String) = "https://image.tmdb.org/t/p/w500$path"
}