package com.qiaoyuang.movie

import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.qiaoyuang.movie.detail.Detail
import com.qiaoyuang.movie.home.Home
import com.qiaoyuang.movie.model.GlobalKoinApplicationConfig
import com.qiaoyuang.movie.search.Search
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(GlobalKoinApplicationConfig) {
        MaterialTheme {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Homepage) {
                composable<Homepage> {
                    Home(
                        navigateToDetail = { movieId -> navController.navigate(DetailedPage(movieId)) },
                        navigateToSearch = { navController.navigate(SearchPage) }
                    )
                }
                composable<DetailedPage>(
                    enterTransition = { slideInHorizontally { weight -> weight } },
                    exitTransition = null,
                    popEnterTransition = null,
                    popExitTransition = { slideOutHorizontally { weight -> weight } },
                ) { backStackEntry ->
                    val id = backStackEntry.toRoute<DetailedPage>().movieId
                    Detail(
                        movieId = id,
                        navigateToNextDetail = { movieId -> navController.navigate(DetailedPage(movieId)) },
                        goBack = { navController.popBackStack() }
                    )
                }
                composable<SearchPage>(
                    enterTransition = { expandVertically() },
                    exitTransition = null,
                    popEnterTransition = null,
                    popExitTransition = { shrinkVertically() },
                ) {
                    Search { movieId -> navController.navigate(DetailedPage(movieId)) }
                }
            }
        }
    }
}