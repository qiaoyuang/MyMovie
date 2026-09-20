package com.qiaoyuang.movie.similar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.qiaoyuang.movie.basicui.*
import com.qiaoyuang.movie.home.MovieItem
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.load_more_failed
import mymovie.composeapp.generated.resources.no_more_results
import mymovie.composeapp.generated.resources.no_result
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
        containerColor = backgroundColor,
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
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            val similarMoviesViewModel = koinViewModel<SimilarMoviesViewModel> { parametersOf(movieId) }
            val movies = similarMoviesViewModel.movies.collectAsLazyPagingItems()
            val refresh = movies.loadState.refresh
            val append = movies.loadState.append

            when {
                refresh is LoadState.Loading -> Loading()
                refresh is LoadState.Error -> Error { movies.retry() }
                movies.itemCount == 0 -> EmptyData(stringResource(Res.string.no_result))
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = rememberLazyListState(),
                    ) {
                        items(
                            count = movies.itemCount,
                            key = movies.itemKey { it.id },
                        ) { index ->
                            movies[index]?.let { MovieItem(it, navigateToDetail) }
                            HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
                        }

                        if (append is LoadState.Loading) item {
                            LoadingMore()
                        }
                    }

                    val noMoreMessage = stringResource(Res.string.no_more_results)
                    val loadMoreFailedMessage = stringResource(Res.string.load_more_failed)
                    LaunchedEffect(append) {
                        when {
                            append is LoadState.Error -> snackbarHostState.showSnackbar(loadMoreFailedMessage)
                            append.endOfPaginationReached -> snackbarHostState.showSnackbar(noMoreMessage)
                        }
                    }
                }
            }
        }
    }
}