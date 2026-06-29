package com.qiaoyuang.movie.model

import com.qiaoyuang.movie.detail.DetailViewModel
import com.qiaoyuang.movie.domain.SimilarMovieUseCase
import com.qiaoyuang.movie.domain.SimilarMovieUseCaseImpl
import com.qiaoyuang.movie.home.HomeViewModel
import com.qiaoyuang.movie.model.APIService.Companion.API_KEY_PARAM
import com.qiaoyuang.movie.model.APIService.Companion.BASE_URL
import com.qiaoyuang.movie.model.APIService.Companion.KEY
import com.qiaoyuang.movie.navigationModule
import com.qiaoyuang.movie.search.SearchViewModel
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

@OptIn(ExperimentalSerializationApi::class)
internal val mainModule = module {
    single<CoroutineDispatcher>(qualifier = GlobalDispatchers.DEFAULT) { Dispatchers.Default }
    single<CoroutineDispatcher>(qualifier = GlobalDispatchers.IO) { Dispatchers.IO }
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
    single<APIService> { KtorService(get()) }
    single<MovieRepository> { MovieRepositoryImpl(get(), get(GlobalDispatchers.DEFAULT)) }
    factory<SimilarMovieUseCase> { SimilarMovieUseCaseImpl(get(), get(GlobalDispatchers.DEFAULT), it.get()) }
    viewModel { HomeViewModel(get(), get(GlobalDispatchers.DEFAULT)) }
    viewModel { SearchViewModel(get(), get(), get(GlobalDispatchers.DEFAULT)) }
    viewModel {
        val movieId = it.get<Long>()
        DetailViewModel(
            repository = get(),
            similarMovieUseCase = get { parametersOf(movieId) },
            movieId = movieId,
        )
    }
    viewModel { SimilarMoviesViewModel(get(), it.get()) }
}

internal val GlobalKoinApplicationConfig: KoinAppDeclaration = {
    modules(mainModule, navigationModule)
}

internal val GlobalKoinConfiguration = koinConfiguration(GlobalKoinApplicationConfig)