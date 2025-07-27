package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.model.mainModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify
import kotlin.test.Test

class KoinTest {

    @Test
    @OptIn(KoinExperimentalAPI::class)
    fun testDI() {
        mainModule.verify(
            injections = injectedParameters(
                definition<HttpClient>(HttpClientEngine::class),
            )
        )
    }
}