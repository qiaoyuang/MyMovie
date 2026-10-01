package com.qiaoyuang.movie.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.qiaoyuang.movie.basicui.*
import com.qiaoyuang.movie.model.APIService
import com.qiaoyuang.movie.model.domain.Movie
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.no_result
import mymovie.composeapp.generated.resources.top_movies
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

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
            // Subscribing is what starts the first load; there is no getTopMovies() to call.
            val movies = homeViewModel.movies.collectAsLazyPagingItems()

            PagedList(
                items = movies,
                snackbarHostState = snackbarHostState,
                emptyMessage = stringResource(Res.string.no_result),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
                }
                // Reading movies[index] is also what tells Paging how far the user has
                // scrolled, so prefetching replaces the old OnBottomReached callback.
                items(
                    count = movies.itemCount,
                    key = movies.itemKey { it.id },
                ) { index ->
                    movies[index]?.let { MovieItem(it, navigateToDetail) }
                    HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
internal fun MovieItem(data: Movie, navigateToDetail: (id: Long) -> Unit) {
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
internal fun Ratting(voteAverage: Double?) {
    Row(modifier = getRattingModifier(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = rattingStar,
            modifier = size12Modifier,
            tint = lightContentColor,
            contentDescription = null,
        )
        Spacer(width6Modifier)
        Text(
            text = voteAverage.formatRating(),
            color = lightContentColor,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}

/**
 * TMDB sends ratings like 8.706; show a single decimal. Kotlin common has no String.format,
 * so round to one decimal manually. A missing rating reads as 0.0, as it did before.
 */
private fun Double?.formatRating(): String {
    val value = this ?: 0.0
    return ((value * 10).roundToInt() / 10.0).toString()
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