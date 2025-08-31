package com.qiaoyuang.movie.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qiaoyuang.movie.basicui.*
import com.qiaoyuang.movie.home.MovieItem
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.LOADING
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.ERROR
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.SUCCESS
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.load_more_failed
import mymovie.composeapp.generated.resources.network_problem
import mymovie.composeapp.generated.resources.no_more_results
import mymovie.composeapp.generated.resources.no_result
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Preview
@Composable
internal fun Search(navigateToDetail: (id: Long) -> Unit) {
    val searchViewModel = koinViewModel<SearchViewModel>()
    LaunchedEffect(Unit) {
        searchViewModel.init()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        containerColor = backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            SearchCard()
            val dataWithState by searchViewModel.finalResultFlow.collectAsStateWithLifecycle()
            val (results, state) = dataWithState
            if (results.isEmpty()) when (state) {
                LOADING -> Loading()
                ERROR -> EmptyData(stringResource(Res.string.network_problem))
                is SUCCESS -> EmptyData(stringResource(Res.string.no_result))
            } else {
                val scrollState = rememberLazyListState()
                scrollState.OnBottomReached {
                    searchViewModel.loadMore()
                }
                LazyColumn(
                    modifier = fillMaxWidthModifier,
                    state = scrollState,
                ) {
                    items(
                        items = results,
                        key = { it.id },
                        itemContent = {
                            MovieItem(it, navigateToDetail)
                            HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
                        },
                    )
                    if (state is LOADING) item {
                        LoadingMore()
                    }
                }
                val strId = when (state) {
                    is SUCCESS if state.isNoMore -> Res.string.no_more_results
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
internal fun SearchCard() {
    val searchViewModel = koinViewModel<SearchViewModel>()
    Column(modifier = horizontalPadding8Modifier) {
        OutlinedCard(
            onClick = {},
            modifier = searchCardModifier,
            border = BorderStroke(1.dp, hintTextColor),
            colors = CardDefaults.outlinedCardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            val searchWordFlow = searchViewModel.searchWordFlow
            val searchWord by searchWordFlow.collectAsStateWithLifecycle()
            TextField(
                value = searchWord,
                onValueChange = {
                    searchViewModel.search(it)
                },
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
                            searchViewModel.prepareGenreList()
                        },
                    ) {
                        Icon(
                            imageVector = filter,
                            contentDescription = null,
                            tint = commonBlueIconColor,
                            modifier = size24Modifier
                        )
                    }
                    val genreList by searchViewModel.showGenreList
                   DropdownMenu(
                        expanded = openDropDownMenu,
                        onDismissRequest = {
                            openDropDownMenu = !openDropDownMenu
                        },
                       modifier = Modifier.background(popWindowBackground)
                    ) {
                       genreList.forEach {
                           FilterItem(it)
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

@Composable
private fun FilterItem(showGenre: SearchViewModel.ShowGenre) {
    val searchViewModel = koinViewModel<SearchViewModel>()
    var isSelected by showGenre.isSelected
    Row(
        modifier = Modifier.clickable {
            isSelected = !isSelected
            searchViewModel.selectGenre(showGenre)
        }.fillMaxWidth(),
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
            text = showGenre.genre.name,
            modifier = Modifier.padding(4.dp),
            color = mainTitleColor,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Spacer(size8Modifier)
    }
}

private val size8Modifier = Modifier.size(8.dp)
private val size24Modifier = Modifier.size(24.dp)
private val horizontalPadding8Modifier = Modifier.padding(horizontal = 8.dp)
private val searchCardModifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
private val checkedIconModifier = Modifier.size(20.dp)