package com.qiaoyuang.movie.basicui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import com.qiaoyuang.movie.model.ui.UIEvent
import kotlinx.coroutines.flow.Flow
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.load_more_failed
import mymovie.composeapp.generated.resources.no_more_results
import org.jetbrains.compose.resources.stringResource

@Composable
@NonRestartableComposable
fun CollectUiEventAndShowSnackBar(
    uiEventFlow: Flow<UIEvent>,
    snackbarHostState: SnackbarHostState,
) {
    val noMoreMessage = stringResource(Res.string.no_more_results)
    val loadMoreFailedMessage = stringResource(Res.string.load_more_failed)
    LaunchedEffect(Unit) {
        uiEventFlow.collect {
            val message = when (it) {
                UIEvent.CommonNoMoreToast -> noMoreMessage
                UIEvent.CommonErrorToast -> loadMoreFailedMessage
                is UIEvent.ShowToast -> it.message
            }
            snackbarHostState.showSnackbar(message = message)
        }
    }
}