@file:OptIn(KoinExperimentalAPI::class)
package com.qiaoyuang.movie

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.qiaoyuang.movie.basicui.MovieTheme
import com.qiaoyuang.movie.basicui.backgroundColor
import com.qiaoyuang.movie.detail.Detail
import com.qiaoyuang.movie.home.Home
import com.qiaoyuang.movie.model.GlobalKoinConfiguration
import com.qiaoyuang.movie.search.Search
import com.qiaoyuang.movie.similar.SimilarMovies
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.KoinApplication
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@Composable
fun App() {
    KoinApplication(GlobalKoinConfiguration) {
        MovieTheme {
            MaterialTheme {
                NavBackStackScope {
                    val backStack = LocalNavBackStack.current
                    NavDisplay(
                        backStack = backStack,
                        modifier = Modifier.background(backgroundColor),
                        onBack = { backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = koinEntryProvider(),
                    )
                }
            }
        }
    }
}

val LocalNavBackStack = compositionLocalOf<NavBackStack<NavKey>> { NavBackStack(mutableStateListOf(Homepage)) }

@Composable
private fun NavBackStackScope(content: @Composable () -> Unit) {
    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Homepage::class, Homepage.serializer())
                    subclass(DetailedPage::class, DetailedPage.serializer())
                    subclass(SearchPage::class, SearchPage.serializer())
                    subclass(SimilarMoviesPage::class, SimilarMoviesPage.serializer())
                }
            }
        },
        Homepage,
    )
    CompositionLocalProvider(
        value = LocalNavBackStack provides backStack,
        content = content,
    )
}

val navigationModule = module {
    navigation<Homepage>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith scaleOut() + fadeOut()
        } + NavDisplay.popTransitionSpec {
            scaleIn() + fadeIn() togetherWith ExitTransition.None
        } + NavDisplay.predictivePopTransitionSpec {
            scaleIn() + fadeIn() togetherWith ExitTransition.None
        }
    ) {
        val backStack = LocalNavBackStack.current
        Home(
            navigateToDetail = { movieId -> backStack.add(DetailedPage(movieId)) },
            navigateToSearch = { backStack.add(SearchPage) }
        )
    }
    navigation<DetailedPage>(
        metadata = NavDisplay.transitionSpec {
            slideInHorizontally { weight -> weight } togetherWith scaleOut() + fadeOut()
        } + NavDisplay.popTransitionSpec {
            scaleIn() + fadeIn() togetherWith slideOutHorizontally { weight -> weight }
        } + NavDisplay.predictivePopTransitionSpec {
            scaleIn() + fadeIn() togetherWith slideOutHorizontally { weight -> weight }
        }
    ) {
        val backStack = LocalNavBackStack.current
        Detail(
            movieId = it.movieId,
            navigateToNextDetail = { movieId -> backStack.add(DetailedPage(movieId)) },
            navigateToAllSimilarMovies = { movieId -> backStack.add(SimilarMoviesPage(movieId)) },
            goBack = { backStack.removeLastOrNull() }
        )
    }
    navigation<SearchPage>(
        metadata = NavDisplay.transitionSpec {
            expandVertically() togetherWith scaleOut() + fadeOut()
        } + NavDisplay.popTransitionSpec {
            scaleIn() + fadeIn() togetherWith shrinkVertically()
        } + NavDisplay.predictivePopTransitionSpec {
            scaleIn() + fadeIn() togetherWith shrinkVertically()
        }
    ) {
        val backStack = LocalNavBackStack.current
        Search(
            navigateToDetail = { movieId -> backStack.add(DetailedPage(movieId)) }
        )
    }
    navigation<SimilarMoviesPage>(
        metadata = NavDisplay.transitionSpec {
            slideInHorizontally { weight -> weight } togetherWith scaleOut() + fadeOut()
        } + NavDisplay.transitionSpec {
            scaleIn() + fadeIn() togetherWith slideOutHorizontally { weight -> weight }
        } + NavDisplay.predictivePopTransitionSpec {
            scaleIn() + fadeIn() togetherWith slideOutHorizontally { weight -> weight }
        }
    ) { similarMoviesPage ->
        val backStack = LocalNavBackStack.current
        SimilarMovies(
            movieId = similarMoviesPage.movieId,
            navigateToDetail = { backStack.add(DetailedPage(it)) },
            goBack = { backStack.removeLastOrNull() },
        )
    }
}