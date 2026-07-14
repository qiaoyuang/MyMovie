package com.qiaoyuang.movie.test

import androidx.lifecycle.SavedStateHandle
import com.qiaoyuang.movie.model.mainModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineDispatcher
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
            extraTypes = listOf(
                SavedStateHandle::class,
                CoroutineDispatcher::class,
            ),
            injections = injectedParameters(
                definition<HttpClient>(HttpClientEngine::class),
            )
        )
    }
}