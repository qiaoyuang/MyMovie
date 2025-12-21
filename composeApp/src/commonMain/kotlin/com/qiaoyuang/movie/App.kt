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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.qiaoyuang.movie.basicui.MovieTheme
import com.qiaoyuang.movie.basicui.backgroundColor
import com.qiaoyuang.movie.detail.Detail
import com.qiaoyuang.movie.home.Home
import com.qiaoyuang.movie.model.GlobalKoinConfiguration
import com.qiaoyuang.movie.search.Search
import com.qiaoyuang.movie.similar.SimilarMovies
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@Composable
fun App() {
    KoinApplication(GlobalKoinConfiguration) {
        MovieTheme {
            MaterialTheme {
                val backStack = koinInject<SnapshotStateList<NavKey>>()
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

val navigationModule = module {
    single<SnapshotStateList<NavKey>> {
        mutableStateListOf(Homepage)
    }
    navigation<Homepage>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith scaleOut() + fadeOut()
        } + NavDisplay.popTransitionSpec {
            scaleIn() + fadeIn() togetherWith ExitTransition.None
        } + NavDisplay.predictivePopTransitionSpec {
            scaleIn() + fadeIn() togetherWith ExitTransition.None
        }
    ) {
        Home(
            navigateToDetail = { movieId -> get<SnapshotStateList<NavKey>>().add(DetailedPage(movieId)) },
            navigateToSearch = { get<SnapshotStateList<NavKey>>().add(SearchPage) }
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
        Detail(
            movieId = it.movieId,
            navigateToNextDetail = { movieId -> get<SnapshotStateList<NavKey>>().add(DetailedPage(movieId)) },
            navigateToAllSimilarMovies = { movieId -> get<SnapshotStateList<NavKey>>().add(SimilarMoviesPage(movieId)) },
            goBack = { get<SnapshotStateList<NavKey>>().removeLastOrNull() }
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
        Search(
            navigateToDetail = { movieId -> get<SnapshotStateList<NavKey>>().add(DetailedPage(movieId)) }
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
        SimilarMovies(
            movieId = similarMoviesPage.movieId,
            navigateToDetail = { get<SnapshotStateList<NavKey>>().add(DetailedPage(it)) },
            goBack = { get<SnapshotStateList<NavKey>>().removeLastOrNull() },
        )
    }
}