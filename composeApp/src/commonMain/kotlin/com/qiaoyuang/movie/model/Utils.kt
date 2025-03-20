package com.qiaoyuang.movie.model

import com.qiaoyuang.movie.detail.DetailViewModel
import com.qiaoyuang.movie.home.HomeViewModel
import com.qiaoyuang.movie.search.SearchViewModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

@OptIn(ExperimentalSerializationApi::class)
internal val mainModule = module {
    single { MovieRepository }
    single { KtorService }
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
    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { DetailViewModel(get(), it.get()) }
}

internal val GlobalKoinApplicationConfig: KoinAppDeclaration = {
    modules(mainModule)
}