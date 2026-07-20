package dev.jotalac.core.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AppVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    // No-op on Android, as scrollbars are handled natively
}
