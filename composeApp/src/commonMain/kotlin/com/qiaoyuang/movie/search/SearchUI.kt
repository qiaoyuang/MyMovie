package com.qiaoyuang.movie.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiaoyuang.movie.basicui.*
import com.qiaoyuang.movie.basicui.hintTextColor
import com.qiaoyuang.movie.home.MovieItem
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.EMPTY
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.ERROR
import com.qiaoyuang.movie.search.SearchViewModel.SearchResultState.SUCCESS
import kotlinx.coroutines.Dispatchers
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.network_problem
import mymovie.composeapp.generated.resources.no_result
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Preview
@Composable
internal fun Search(navigateToDetail: (id: Long) -> Unit) {
    val searchViewModel = koinViewModel<SearchViewModel>()
    Column {
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars))
        SearchCard()
        when (val searchResult = searchViewModel.finalResultFlow.collectAsState(Dispatchers.Main).value) {
            EMPTY -> {
                if (searchViewModel.searchWord.isNotEmpty())
                    EmptyData(stringResource(Res.string.no_result))
            }
            ERROR -> {
                EmptyData(stringResource(Res.string.network_problem))
            }
            is SUCCESS -> {
                LazyColumn(fillMaxWidthModifier) {
                    items(
                        items = searchResult.value,
                        key = { it.id },
                        itemContent = {
                            MovieItem(it, navigateToDetail)
                            HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
                        },
                    )
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
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            TextField(
                value = searchViewModel.searchWord,
                onValueChange = {
                    with(searchViewModel) {
                        searchWord = it
                        query(it)
                    }
                },
                modifier = fillMaxWidthModifier,
                leadingIcon = {
                    Icon(
                        imageVector = search,
                        contentDescription = null,
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
                            modifier = size24Modifier
                        )
                    }
                    val genreList by searchViewModel.showGenreList
                   DropdownMenu(
                        expanded = openDropDownMenu,
                        onDismissRequest = {
                            openDropDownMenu = !openDropDownMenu
                        },
                    ) {
                       genreList.forEach {
                           FilterItem(it)
                       }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = hintTextColor,
                    unfocusedTextColor = hintTextColor,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = hintTextColor,
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