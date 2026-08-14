package dev.jotalac.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AppVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    // No-op on iOS, as scrollbars are handled natively
}

@Composable
actual fun AppVerticalScrollbar(
    listState: ScrollState,
    modifier: Modifier
) {
    // No-op on iOS, as scrollbars are handled natively
}
