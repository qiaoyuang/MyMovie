package com.qiaoyuang.movie.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.qiaoyuang.movie.basicui.*
import com.qiaoyuang.movie.home.MovieItem
import com.qiaoyuang.movie.model.domain.MovieGenre
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.load_more_failed
import mymovie.composeapp.generated.resources.no_more_results
import mymovie.composeapp.generated.resources.no_result
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun Search(navigateToDetail: (id: Long) -> Unit) {
    val searchViewModel = koinViewModel<SearchViewModel>()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        containerColor = backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // The screen-level composable is the only one that touches the ViewModel: it reads
            // state and forwards events, so SearchCard below is a plain function of its inputs.
            val searchWord by searchViewModel.searchWordFlow.collectAsStateWithLifecycle()
            val genreFilterState by searchViewModel.genreFilterState.collectAsStateWithLifecycle()
            SearchCard(
                searchWord = searchWord,
                onSearchWordChange = { searchViewModel.search(it) },
                genreFilterState = genreFilterState,
                onFilterMenuOpen = { searchViewModel.prepareGenreList() },
                onToggleGenre = { searchViewModel.toggleGenre(it) },
            )
            val movies = searchViewModel.movies.collectAsLazyPagingItems()
            val refresh = movies.loadState.refresh
            val append = movies.loadState.append

            when {
                refresh is LoadState.Loading -> Loading()
                refresh is LoadState.Error -> EmptyData(refresh.error.toErrorKind().message())
                movies.itemCount == 0 -> EmptyData(stringResource(Res.string.no_result))
                else -> {
                    LazyColumn(
                        modifier = fillMaxWidthModifier,
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

/**
 * Receives state and reports events instead of fetching SearchViewModel itself. It still
 * resolved the right instance before — only because it happened to sit under the same
 * ViewModelStoreOwner as Search() — but it could not be previewed or tested without a Koin
 * container, and its empty signature hid every one of those dependencies.
 *
 * Whether the genre menu is open stays local: that is UI element state, which nothing
 * outside this composable needs to read or survive.
 */
@Composable
private fun SearchCard(
    searchWord: String,
    onSearchWordChange: (String) -> Unit,
    genreFilterState: SearchViewModel.GenreFilterState,
    onFilterMenuOpen: () -> Unit,
    onToggleGenre: (genreId: Int) -> Unit,
) {
    Column(modifier = horizontalPadding8Modifier) {
        OutlinedCard(
            onClick = {},
            modifier = searchCardModifier,
            border = BorderStroke(1.dp, hintTextColor),
            colors = CardDefaults.outlinedCardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            TextField(
                value = searchWord,
                onValueChange = onSearchWordChange,
                modifier = fillMaxWidthModifier,
                leadingIcon = {
                    Icon(
                        imageVector = search,
                        contentDescription = null,
                        tint = commonBlueIconColor,
                        modifier = size24Modifier
                    )
                },
                trailingIcon = {
                    var openDropDownMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            openDropDownMenu = true
                            onFilterMenuOpen()
                        },
                    ) {
                        Icon(
                            imageVector = filter,
                            contentDescription = null,
                            tint = commonBlueIconColor,
                            modifier = size24Modifier
                        )
                    }
                   DropdownMenu(
                        expanded = openDropDownMenu,
                        onDismissRequest = {
                            openDropDownMenu = !openDropDownMenu
                        },
                       modifier = Modifier.background(popWindowBackground)
                    ) {
                       // State flows down (genre + isSelected), the click event flows up.
                       // isSelected is derived from the single selectedIds set, so it always
                       // matches the filter that is actually applied to the results.
                       genreFilterState.genres.forEach { genre ->
                           FilterItem(
                               genre = genre,
                               isSelected = genre.id in genreFilterState.selectedIds,
                               onClick = { onToggleGenre(genre.id) },
                           )
                       }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = onSurfaceColor,
                    unfocusedTextColor = onSurfaceColor,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = commonBlueIconColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = commonBlueIconColor,
                    unfocusedLeadingIconColor = commonBlueIconColor,
                    focusedTrailingIconColor = commonBlueIconColor,
                    unfocusedTrailingIconColor = commonBlueIconColor,
                )
            )
        }
    }
}

// Takes plain stable parameters instead of reaching for the ViewModel itself, so it
// owns no state, is skipped on recomposition when its genre is not the one that changed,
// and no longer needs a coroutine collector per item.
@Composable
private fun FilterItem(
    genre: MovieGenre,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(size8Modifier)
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = checkedIconModifier,
                tint = commonBlueIconColor,
            )
        } else {
            Spacer(checkedIconModifier)
        }
        Spacer(size8Modifier)
        Text(
            text = genre.name,
            modifier = Modifier.padding(4.dp),
            color = mainTitleColor,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Spacer(size8Modifier)
    }
}

@Preview
@Composable
private fun SearchCardPreview() {
    MovieTheme {
        SearchCard(
            searchWord = "Inception",
            onSearchWordChange = {},
            genreFilterState = SearchViewModel.GenreFilterState(
                genres = listOf(MovieGenre(28, "Action"), MovieGenre(878, "Science Fiction")),
                selectedIds = setOf(878),
            ),
            onFilterMenuOpen = {},
            onToggleGenre = {},
        )
    }
}

@Preview
@Composable
private fun FilterItemPreview() {
    MovieTheme {
        Column {
            FilterItem(genre = MovieGenre(28, "Action"), isSelected = false, onClick = {})
            FilterItem(genre = MovieGenre(878, "Science Fiction"), isSelected = true, onClick = {})
        }
    }
}

private val size8Modifier = Modifier.size(8.dp)
private val size24Modifier = Modifier.size(24.dp)
private val horizontalPadding8Modifier = Modifier.padding(horizontal = 8.dp)
private val searchCardModifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
private val checkedIconModifier = Modifier.size(20.dp)