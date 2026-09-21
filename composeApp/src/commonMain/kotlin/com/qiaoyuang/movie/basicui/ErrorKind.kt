package com.qiaoyuang.movie.basicui

import androidx.compose.runtime.Composable
import com.qiaoyuang.movie.model.MovieDataException
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.load_failed
import mymovie.composeapp.generated.resources.network_problem
import org.jetbrains.compose.resources.stringResource

/**
 * How finely the UI distinguishes failures. The data layer keeps more detail — Http carries the
 * status code, Parse and Unknown are separate — but a network failure is the only one the user
 * can act on, so it is the only split shown. Refining it later, say a 404 on the detail screen,
 * only touches this file.
 */
internal enum class ErrorKind { Network, Other }

/**
 * Where a Throwable stops. DetailViewModel calls this so its UI state carries no exception. The
 * paged screens call it on LoadState.Error, because Paging hands the throwable straight to the
 * composable without it ever passing through a ViewModel.
 */
internal fun Throwable.toErrorKind(): ErrorKind =
    if (this is MovieDataException.Network) ErrorKind.Network else ErrorKind.Other

@Composable
internal fun ErrorKind.message(): String = stringResource(
    when (this) {
        ErrorKind.Network -> Res.string.network_problem
        ErrorKind.Other -> Res.string.load_failed
    }
)
