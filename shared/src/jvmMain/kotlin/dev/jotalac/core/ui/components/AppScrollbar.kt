package dev.jotalac.core.ui.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun AppVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    VerticalScrollbar(
        modifier = modifier.width(6.dp),
        adapter = rememberScrollbarAdapter(scrollState = listState),
        style = defaultScrollbarStyle().copy(
            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    )
}
