package com.qiaoyuang.movie.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
// noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.qiaoyuang.movie.basicui.*
import com.qiaoyuang.movie.basicui.containerColor
import kotlinx.coroutines.Dispatchers

import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.ERROR
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.SHOW
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.LOADING
import com.qiaoyuang.movie.model.APIService
import com.qiaoyuang.movie.model.ApiMovie
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.load_more_failed
import mymovie.composeapp.generated.resources.no_more_results
import mymovie.composeapp.generated.resources.top_movies
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Home(
    navigateToDetail: (id: Long) -> Unit,
    navigateToSearch: () -> Unit,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.top_movies),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                },
                actions = {
                    IconButton(
                        onClick = navigateToSearch,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = scrolledContainerColor,
                    titleContentColor = lightContentColor,
                    actionIconContentColor = lightContentColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        val listState = rememberLazyListState()
        listState.OnBottomReached {
            homeViewModel.getTopMovies(true)
        }
        when (val movieState = homeViewModel.movieState.collectAsState(Dispatchers.Main).value) {
            LOADING -> Loading()
            ERROR -> Error {
                homeViewModel.getTopMovies(false)
            }
            is SHOW -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                ) {
                    item {
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
                    }
                    items(
                        items = movieState.value,
                        key = { it.id },
                        itemContent = {
                            MovieItem(it, navigateToDetail)
                            HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
                        },
                    )

                    if (homeViewModel.isLoading) item {
                        LoadingMore()
                    }
                }

                if (movieState.loadingMoreState != LoadingMoreState.SUCCESS) {
                    val strId = when (movieState.loadingMoreState) {
                        LoadingMoreState.FAIL -> Res.string.load_more_failed
                        LoadingMoreState.NO_MORE -> Res.string.no_more_results
                        else -> throw IllegalStateException("Impossible")
                    }
                    val snackBarMessage = stringResource(strId)
                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar(message = snackBarMessage)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MovieItem(data: ApiMovie, navigateToDetail: (id: Long) -> Unit) {
    Row(padding16Modifier.clickable { navigateToDetail(data.id) }) {
        Column {
            data.posterPath?.takeIf { it.isNotEmpty() }?.let {
                AsyncImage(
                    model = APIService buildImageUrl it,
                    contentDescription = null,
                    modifier = asyncImageModifier
                )
            }
            Spacer(height10Modifier)
            Ratting(data.voteAverage)
        }
        Spacer(width16Modifier)
        Column {
            Text(
                text = data.title,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                modifier = fillMaxWidthModifier,
            )
            Spacer(height4Modifier)
            Text(
                text = data.overview,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = fillMaxWidthModifier,
            )
        }
    }
}

@Composable
internal fun Ratting(voteAverage: String?) {
    Row(modifier = rattingModifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            imageVector = rattingStar,
            modifier = size12Modifier,
            contentDescription = null,
        )
        Spacer(width6Modifier)
        Text(
            text = voteAverage ?: "0.0",
            color = lightContentColor,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}

private val padding16Modifier = Modifier.padding(16.dp)
private val asyncImageModifier = Modifier.width(92.dp).height(134.dp)
private val rattingModifier = Modifier
    .background(color = mainTitleColor, shape = RoundedCornerShape(4.dp))
    .padding(6.dp)
private val size12Modifier = Modifier.size(12.dp)
private val width6Modifier = Modifier.width(6.dp)
private val width16Modifier = Modifier.width(16.dp)