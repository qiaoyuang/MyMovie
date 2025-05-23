package com.qiaoyuang.movie.similar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiaoyuang.movie.basicui.Error
import com.qiaoyuang.movie.basicui.Loading
import com.qiaoyuang.movie.basicui.LoadingMore
import com.qiaoyuang.movie.basicui.OnBottomReached
import com.qiaoyuang.movie.basicui.lightContentColor
import com.qiaoyuang.movie.basicui.scrolledContainerColor
import com.qiaoyuang.movie.home.MovieItem
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel.SimilarMoviesState.LOADING
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel.SimilarMoviesState.SUCCESS
import com.qiaoyuang.movie.similar.SimilarMoviesViewModel.SimilarMoviesState.ERROR
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.can_not_load_more
import mymovie.composeapp.generated.resources.similar_movies
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SimilarMovies(
    movieId: Long,
    navigateToDetail: (id: Long) -> Unit,
    goBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.similar_movies),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scrolledContainerColor,
                    scrolledContainerColor = scrolledContainerColor,
                    navigationIconContentColor = lightContentColor,
                    titleContentColor = lightContentColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        val similarMoviesViewModel = koinViewModel<SimilarMoviesViewModel> { parametersOf(movieId) }
        val scrollState = rememberLazyListState()
        scrollState.OnBottomReached {
            similarMoviesViewModel.getSimilarMovies(true)
        }
        when (val movieState = similarMoviesViewModel.movieState.collectAsState().value) {
            LOADING -> Loading()
            ERROR -> Error {
                similarMoviesViewModel.getSimilarMovies(false)
            }
            is SUCCESS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = scrollState,
                ) {
                    items(
                        items = movieState.value,
                        key = { it.id },
                        itemContent = {
                            MovieItem(it, navigateToDetail)
                            HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
                        }
                    )
                    if (similarMoviesViewModel.isLoading) item {
                        LoadingMore()
                    }
                }
                if (movieState.isLoadMoreFail) {
                    val snackBarMessage = stringResource(Res.string.can_not_load_more)
                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar(message = snackBarMessage)
                    }
                }
            }
        }
    }
}