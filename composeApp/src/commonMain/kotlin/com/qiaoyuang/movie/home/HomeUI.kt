package com.qiaoyuang.movie.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.qiaoyuang.movie.basicui.*
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.ERROR
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.SUCCESS
import com.qiaoyuang.movie.home.HomeViewModel.TopMoviesState.LOADING
import com.qiaoyuang.movie.model.APIService
import com.qiaoyuang.movie.model.ApiMovie
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.load_more_failed
import mymovie.composeapp.generated.resources.no_more_results
import mymovie.composeapp.generated.resources.no_result
import mymovie.composeapp.generated.resources.top_movies
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Home(
    navigateToDetail: (id: Long) -> Unit,
    navigateToSearch: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = backgroundColor,
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
                    ThemeSelectionButton()
                    IconButton(
                        onClick = navigateToSearch,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = scrolledContainerColor,
                    titleContentColor = lightContentColor,
                    actionIconContentColor = lightContentColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingVales ->
        Column(modifier = Modifier.padding(paddingVales)) {
            val homeViewModel = koinViewModel<HomeViewModel>()
            LaunchedEffect(Unit) {
                homeViewModel.getTopMovies()
            }

            val movieState by homeViewModel.movieState.collectAsStateWithLifecycle()
            if (movieState.data.isEmpty()) when (movieState) {
                is LOADING-> Loading()
                is ERROR -> Error { homeViewModel.getTopMovies() }
                is SUCCESS -> EmptyData(stringResource(Res.string.no_result))
            } else {
                val scrollState = rememberLazyListState()
                scrollState.OnBottomReached {
                    homeViewModel.getTopMovies()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = scrollState,
                ) {
                    item {
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
                    }
                    items(
                        items = movieState.data,
                        key = { it.id },
                        itemContent = {
                            MovieItem(it, navigateToDetail)
                            HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
                        },
                    )

                    if (movieState is LOADING) item {
                        LoadingMore()
                    }
                }
                val strId = when (movieState) {
                    is SUCCESS if (movieState as SUCCESS).isNoMore-> Res.string.no_more_results
                    is ERROR -> Res.string.load_more_failed
                    else -> null
                }
                strId?.let {
                    val snackBarMessage = stringResource(it)
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
                color = mainTitleColor,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                modifier = fillMaxWidthModifier,
            )
            Spacer(height4Modifier)
            Text(
                text = data.overview,
                color = contentTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = fillMaxWidthModifier,
            )
        }
    }
}

@Composable
internal fun Ratting(voteAverage: String?) {
    Row(modifier = getRattingModifier(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = rattingStar,
            modifier = size12Modifier,
            tint = lightContentColor,
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

@Composable
private fun ThemeSelectionButton() {
    var showDropDown by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { showDropDown = true }
        ) {
            Icon(
                imageVector = when (MovieTheme.themeMode) {
                    ThemeMode.LIGHT -> Icons.Outlined.LightMode
                    ThemeMode.DARK -> Icons.Outlined.DarkMode
                    ThemeMode.FOLLOW_SYSTEM -> Icons.Outlined.Palette
                },
                contentDescription = "Theme selection",
            )
        }
        
        DropdownMenu(
            expanded = showDropDown,
            onDismissRequest = { showDropDown = false },
            modifier = Modifier.background(popWindowBackground),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Light",
                        color = mainTitleColor,
                    )
                },
                onClick = {
                    MovieTheme.setThemeMode(ThemeMode.LIGHT)
                    showDropDown = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.LightMode,
                        contentDescription = null,
                        tint = commonBlueIconColor,
                    )
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Dark",
                        color = mainTitleColor,
                    )
                },
                onClick = {
                    MovieTheme.setThemeMode(ThemeMode.DARK)
                    showDropDown = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = commonBlueIconColor,
                    )
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Follow System",
                        color = mainTitleColor,
                    )
                },
                onClick = {
                    MovieTheme.setThemeMode(ThemeMode.FOLLOW_SYSTEM)
                    showDropDown = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = commonBlueIconColor,
                    )
                }
            )
        }
    }
}

private val padding16Modifier = Modifier.padding(16.dp)
private val asyncImageModifier = Modifier.width(92.dp).height(134.dp)
@Composable
private fun getRattingModifier() = Modifier
    .background(color = ratingBackgroundColor, shape = RoundedCornerShape(4.dp))
    .padding(6.dp)
private val size12Modifier = Modifier.size(12.dp)
private val width6Modifier = Modifier.width(6.dp)
private val width16Modifier = Modifier.width(16.dp)