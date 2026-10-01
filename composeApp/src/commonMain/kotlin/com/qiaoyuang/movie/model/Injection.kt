package com.qiaoyuang.movie.model

import com.qiaoyuang.movie.detail.DetailViewModel
import com.qiaoyuang.movie.domain.SimilarMovieUseCase
import com.qiaoyuang.movie.domain.SimilarMovieUseCaseImpl
import com.qiaoyuang.movie.home.HomeViewModel
import com.qiaoyuang.movie.model.APIService.Companion.API_KEY_PARAM
import com.qiaoyuang.movie.model.APIService.Companion.BASE_URL
import com.qiaoyuang.movie.model.APIService.Companion.KEY
import com.qiaoyuang.movie.model.local.MovieLocalDataSource
import com.qiaoyuang.movie.model.local.MovieLocalDataSourceImpl
import com.qiaoyuang.movie.model.local.createMovieDatabase
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
import org.koin.core.context.startKoin
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
    // One connection for the process lifetime: a sqllin Database wraps a single connection and
    // already serialises statement execution, so re-opening per query would only add cost.
    single { createMovieDatabase() }
    single<MovieLocalDataSource> { MovieLocalDataSourceImpl(get(), get(GlobalDispatchers.IO)) }
    single<MovieRepository> { MovieRepositoryImpl(get(), get(), get(GlobalDispatchers.DEFAULT)) }
    factory<SimilarMovieUseCase> { SimilarMovieUseCaseImpl(get(), get(GlobalDispatchers.DEFAULT), it.get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel {
        val movieId = it.get<Long>()
        DetailViewModel(
            repository = get(),
            similarMovieUseCase = get { parametersOf(movieId) },
            movieId = movieId,
        )
    }
    viewModel { SimilarMoviesViewModel(get(), get(), it.get()) }
}

/**
 * Starts the Koin container. Reached only through each platform's setupApp(), which the app
 * entry points call instead of the App() composable doing it, so the graph also exists on code
 * paths that never build any UI — an AppFunction call binds a service in the app's process
 * without creating an Activity. Internal so an entry point cannot start Koin while skipping
 * the rest of setupApp().
 */
internal fun initKoin() {
    startKoin {
        modules(mainModule, navigationModule)
    }
}