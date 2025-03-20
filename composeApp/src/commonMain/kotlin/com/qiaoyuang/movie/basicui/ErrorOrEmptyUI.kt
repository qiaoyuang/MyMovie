package com.qiaoyuang.movie.basicui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.network_problem
import mymovie.composeapp.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

private val fillMaxSizeModifier = Modifier.fillMaxSize()

@Composable
internal fun Error(message: String = stringResource(Res.string.network_problem), onClick: () -> Unit) {
    Column(
        modifier = fillMaxSizeModifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = hintTextColor,
        )
        Spacer(modifier = height16Modifier)
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = lightContentColor,
            ),
       ) {
            Text(
                text = stringResource(Res.string.retry),
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun EmptyData(message: String) {
    Box(
        modifier = fillMaxSizeModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = hintTextColor,
        )
    }
}

@Composable
internal fun Loading() {
    Box(
        modifier = fillMaxSizeModifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = scrolledContainerColor,
        )
    }
}

@Composable
internal fun LoadingMore() {
    Box(
        modifier = loadingMoreBoxModifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = size24Modifier)
    }
}

private val loadingMoreBoxModifier = Modifier.fillMaxWidth().padding(16.dp)
private val size24Modifier = Modifier.size(24.dp)