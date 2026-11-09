package dev.jotalac.core.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme

@Composable
actual fun AppVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    VerticalScrollbar(
        modifier = modifier.width(6.dp),
        adapter = rememberScrollbarAdapter(scrollState = listState),
        style = defaultScrollbarStyle().copy(
            unhoverColor = AppTheme.colors.text.copy(alpha = 0.2f),
            hoverColor = AppTheme.colors.text.copy(alpha = 0.7f)
        )
    )
}

@Composable
actual fun AppVerticalScrollbar(
    listState: ScrollState,
    modifier: Modifier
) {
    VerticalScrollbar(
        modifier = modifier.width(6.dp),
        adapter = rememberScrollbarAdapter(scrollState = listState),
        style = defaultScrollbarStyle().copy(
            unhoverColor = AppTheme.colors.text.copy(alpha = 0.2f),
            hoverColor = AppTheme.colors.text.copy(alpha = 0.7f)
        )
    )
}


@Composable
actual fun AppHorizontalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    HorizontalScrollbar(
        modifier = modifier.height(4.dp),
        adapter = rememberScrollbarAdapter(scrollState = listState),
        style = defaultScrollbarStyle().copy(
            unhoverColor = AppTheme.colors.text.copy(alpha = 0.2f),
            hoverColor = AppTheme.colors.text.copy(alpha = 0.7f)
        )
    )
}