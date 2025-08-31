package com.qiaoyuang.movie

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.qiaoyuang.movie.basicui.MovieTheme
import com.qiaoyuang.movie.basicui.backgroundColor
import com.qiaoyuang.movie.detail.Detail
import com.qiaoyuang.movie.home.Home
import com.qiaoyuang.movie.model.GlobalKoinApplicationConfig
import com.qiaoyuang.movie.search.Search
import com.qiaoyuang.movie.similar.SimilarMovies
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(GlobalKoinApplicationConfig) {
        MovieTheme {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(
                    modifier = Modifier.background(backgroundColor),
                    navController = navController,
                    startDestination = Homepage,
                ) {
                    composable<Homepage>(
                        exitTransition = { scaleOut() + fadeOut() },
                        popEnterTransition = { scaleIn() + fadeIn() },
                    ) {
                        Home(
                            navigateToDetail = { movieId -> navController.navigate(DetailedPage(movieId)) },
                            navigateToSearch = { navController.navigate(SearchPage) }
                        )
                    }
                    composable<DetailedPage>(
                        enterTransition = { slideInHorizontally { weight -> weight } },
                        exitTransition = { scaleOut() + fadeOut() },
                        popEnterTransition = { scaleIn() + fadeIn() },
                        popExitTransition = { slideOutHorizontally { weight -> weight } },
                    ) { backStackEntry ->
                        val id = backStackEntry.toRoute<DetailedPage>().movieId
                        Detail(
                            movieId = id,
                            navigateToNextDetail = { movieId -> navController.navigate(DetailedPage(movieId)) },
                            navigateToAllSimilarMovies = { movieId -> navController.navigate(SimilarMoviesPage(movieId)) },
                            goBack = { navController.popBackStack() }
                        )
                    }
                    composable<SearchPage>(
                        enterTransition = { expandVertically() },
                        exitTransition = { scaleOut() + fadeOut() },
                        popEnterTransition = { scaleIn() + fadeIn() },
                        popExitTransition = { shrinkVertically() },
                    ) {
                        Search { movieId -> navController.navigate(DetailedPage(movieId)) }
                    }

                    composable<SimilarMoviesPage>(
                        enterTransition = { slideInHorizontally { weight -> weight } },
                        exitTransition = { scaleOut() + fadeOut() },
                        popEnterTransition = { scaleIn() + fadeIn() },
                        popExitTransition = { slideOutHorizontally { weight -> weight } },
                    ) { backStackEntry ->
                        val id = backStackEntry.toRoute<SimilarMoviesPage>().movieId
                        SimilarMovies(
                            movieId = id,
                            navigateToDetail = { navController.navigate(DetailedPage(it)) },
                            goBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}