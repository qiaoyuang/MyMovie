package com.qiaoyuang.movie.model

import com.qiaoyuang.movie.detail.DetailViewModel
import com.qiaoyuang.movie.home.HomeViewModel
import com.qiaoyuang.movie.model.APIService.Companion.API_KEY_PARAM
import com.qiaoyuang.movie.model.APIService.Companion.BASE_URL
import com.qiaoyuang.movie.model.APIService.Companion.KEY
import com.qiaoyuang.movie.search.SearchViewModel
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

@OptIn(ExperimentalSerializationApi::class)
internal val mainModule = module {
    single<APIService> { KtorService(get()) }
    single<MovieRepository> { MovieRepositoryImpl(get()) }
    single {
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
            coerceInputValues = true
            allowTrailingComma = true
            allowStructuredMapKeys = true
        }
    }
    single {
        HttpClient(ktorEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(get())
            }
            defaultRequest {
                url(BASE_URL)
                url.parameters.append(API_KEY_PARAM, KEY)
            }
        }
    }
    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { DetailViewModel(get(), it.get()) }
    viewModel { SimilarMoviesViewModel(get(), it.get()) }
}

internal val GlobalKoinApplicationConfig: KoinAppDeclaration = {
    modules(mainModule)
}