package dev.jotalac.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
)

@Composable
expect fun AppVerticalScrollbar(
    listState: ScrollState,
    modifier: Modifier = Modifier
)


@Composable
expect fun AppHorizontalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
)