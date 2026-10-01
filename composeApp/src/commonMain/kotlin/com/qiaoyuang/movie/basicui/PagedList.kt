package com.qiaoyuang.movie.basicui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import mymovie.composeapp.generated.resources.Res
import mymovie.composeapp.generated.resources.load_more_failed
import mymovie.composeapp.generated.resources.no_more_results
import org.jetbrains.compose.resources.stringResource

/**
 * The load-state shell shared by the paged screens. Factored out rather than copied because the
 * reasoning behind it is subtle enough that two versions would drift apart:
 *
 * - The full-screen loading and error states are gated on the list being empty. Once a cache can
 *   answer first, a refresh happens with rows already on screen, and showing a spinner or an
 *   error page then would blank out the very list the user is reading.
 * - The pull indicator watches the mediator's own refresh, not the combined one. Every page a
 *   RemoteMediator appends invalidates the local source, and Paging answers that with a source
 *   refresh — so the combined state goes Loading whenever the user simply scrolls far enough to
 *   need another page.
 * - A failed refresh is reported in a snackbar instead of replacing the content it failed to
 *   update, because stale rows still beat an error page.
 *
 * Works for a plain network-backed Pager too: with no mediator, [LazyPagingItems.loadState]'s
 * mediator state is null, so the indicator simply never shows.
 */
@Composable
internal fun <T : Any> PagedList(
    items: LazyPagingItems<T>,
    snackbarHostState: SnackbarHostState,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val refresh = items.loadState.refresh
    val append = items.loadState.append
    val hasItems = items.itemCount > 0
    val networkRefresh = items.loadState.mediator?.refresh

    when {
        !hasItems && refresh is LoadState.Loading -> Loading()
        !hasItems && refresh is LoadState.Error ->
            Error(message = refresh.error.toErrorKind().message()) { items.retry() }
        !hasItems -> EmptyData(emptyMessage)
        else -> {
            PullToRefreshBox(
                isRefreshing = networkRefresh is LoadState.Loading,
                // Discards the cached list and re-reads page one, which is the only way to see
                // new rows before the cache's TTL expires.
                onRefresh = items::refresh,
            ) {
                LazyColumn(modifier = modifier, state = rememberLazyListState()) {
                    content()
                    if (append is LoadState.Loading) item { LoadingMore() }
                }
            }

            if (networkRefresh is LoadState.Error) {
                val refreshFailedMessage = networkRefresh.error.toErrorKind().message()
                LaunchedEffect(networkRefresh) {
                    snackbarHostState.showSnackbar(refreshFailedMessage)
                }
            }

            val noMoreMessage = stringResource(Res.string.no_more_results)
            val loadMoreFailedMessage = stringResource(Res.string.load_more_failed)
            // Keyed on the state itself, so a later failure fires again rather than only once.
            LaunchedEffect(append) {
                when {
                    // endOfPaginationReached lives on LoadState itself and is always false for
                    // Loading and Error, so no type check is needed here.
                    append is LoadState.Error -> snackbarHostState.showSnackbar(loadMoreFailedMessage)
                    append.endOfPaginationReached -> snackbarHostState.showSnackbar(noMoreMessage)
                }
            }
        }
    }
}
