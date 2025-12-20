package com.qiaoyuang.movie.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.qiaoyuang.movie.detail.DetailViewModel
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.compose.navigation3.EntryProviderInstaller
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.scope.Scope
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

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
    modules(mainModule, navigationModule)
}

internal val GlobalKoinConfiguration = koinConfiguration(GlobalKoinApplicationConfig)

@OptIn(KoinExperimentalAPI::class)
inline fun <reified T : Any> Module.navigationWithLifecycle(
    metadata: Map<String, Any> = emptyMap(),
    noinline definition: @Composable Scope.(T) -> Unit,
): KoinDefinition<EntryProviderInstaller> =
    navigation(
        metadata = metadata,
        definition = { param: T ->
            val viewModelScopeOwner = remember { NavigationScopeOwner() }
            DisposableEffect(Unit) {
                onDispose {
                    viewModelScopeOwner.clear()
                }
            }
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelScopeOwner) {
                definition(param)
            }
        },
    )

class NavigationScopeOwner : ViewModelStoreOwner {

    override val viewModelStore = ViewModelStore()

    fun clear() = viewModelStore.clear()
}