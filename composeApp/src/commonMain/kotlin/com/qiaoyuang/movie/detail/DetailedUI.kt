package com.qiaoyuang.movie.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
// noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.qiaoyuang.movie.basicui.*
import com.qiaoyuang.movie.basicui.commonBlueTextColor
import com.qiaoyuang.movie.basicui.contentTextColor
import com.qiaoyuang.movie.basicui.sectionTitleColor
import com.qiaoyuang.movie.detail.DetailViewModel.MovieDetailState
import com.qiaoyuang.movie.home.Ratting
import com.qiaoyuang.movie.model.APIService
import com.qiaoyuang.movie.model.ApiMovie
import com.qiaoyuang.movie.model.SimilarMovieShowModel
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.movie_detail
import mymovie.composeapp.generated.resources.similar_movies
import mymovie.composeapp.generated.resources.view_all
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Detail(
    movieId: Long,
    navigateToNextDetail: (Long) -> Unit,
    navigateToAllSimilarMovies: (Long) -> Unit,
    goBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.movie_detail),
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
    ) {
        val detailViewModel = koinViewModel<DetailViewModel> { parametersOf(movieId) }
        LaunchedEffect(Unit) {
            detailViewModel.updateUI()
        }
        when (val detailState = detailViewModel.movieDetailState.collectAsStateWithLifecycle().value) {
            MovieDetailState.LOADING -> Loading()
            MovieDetailState.ERROR -> Error { detailViewModel.updateUI() }
            is MovieDetailState.SUCCESS -> {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MovieDetail(detailState.movie)
                    Spacer(height24Modifier)
                    detailState.similarMovies?.let {
                        SimilarMovies(movieId,it, navigateToNextDetail, navigateToAllSimilarMovies)
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieDetail(movie: ApiMovie) {
    Column(containerModifier) {
        Spacer(height24Modifier)
        movie.backdropPath?.let {
            AsyncImage(
                model = APIService buildImageUrl it,
                contentDescription = null,
                modifier = movieBackDropModifier,
            )
        }
        Spacer(height24Modifier)
        Text(
            text = movie.title,
            color = mainTitleColor,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(height8Modifier)
        Text(
            text = movie.overview,
            color = contentTextColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun SimilarMovies(
    movieId: Long,
    models: List<SimilarMovieShowModel>,
    navigateToNextDetail: (Long) -> Unit,
    navigateToAllSimilarMovies: (Long) -> Unit,
) {
    Column(containerModifier) {
        Row(
            modifier = fillMaxWidthModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(Res.string.similar_movies),
                color = sectionTitleColor,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.clickable { navigateToAllSimilarMovies(movieId) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.view_all),
                    modifier = endPadding8Modifier,
                    color = commonBlueTextColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Image(
                    imageVector = rightArrow,
                    modifier = size14Modifier,
                    contentDescription = null,
                )
            }
        }
        Spacer(height16Modifier)
        LazyRow {
            items(
                items = models,
                key = { it.id },
                itemContent = {
                    SimilarMovieItem(it, navigateToNextDetail)
                    VerticalDivider(
                        modifier = horizontal8PaddingModifier,
                        thickness = 0.dp,
                        color = Color.Transparent,
                    )
                }
            )
        }
        Spacer(height16Modifier)
    }

}
@Composable
private fun SimilarMovieItem(model: SimilarMovieShowModel, navigateToNextDetail: (Long) -> Unit) {
    Column(Modifier.clickable { navigateToNextDetail(model.id) }) {
        model.posterPath?.let { path ->
            AsyncImage(
                model = APIService buildImageUrl path,
                contentDescription = null,
                modifier = posterModifier
            )
        }
        Spacer(height4Modifier)
        Text(
            text = model.title,
            color = sectionTitleColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = itemTextModifier,
        )
        Text(
            text = model.genres,
            color = hintTextColor,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = itemTextModifier,
        )
        Spacer(height10Modifier)
        Ratting(model.voteAverage)
    }
}

private val posterModifier = Modifier.width(144.dp).height(212.dp)
private val itemTextModifier = Modifier.widthIn(max = 144.dp)
private val height24Modifier = Modifier.height(24.dp)
// private val height48Modifier = Modifier.height(48.dp)
private val containerModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
private val endPadding8Modifier = Modifier.padding(end = 8.dp)
private val size14Modifier = Modifier.size(14.dp)
private val movieBackDropModifier = Modifier.fillMaxWidth().aspectRatio(1.7777f).background(Color(0xFFFF4081))
private val height8Modifier = Modifier.height(8.dp)
private val horizontal8PaddingModifier = Modifier.padding(horizontal = 4.dp)