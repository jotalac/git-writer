package dev.jotalac.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun DesktopContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset,
    content: @Composable () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clip(RoundedCornerShape(6.dp))
            .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f), shape = RoundedCornerShape(6.dp))
            .padding(5.dp)
            .width(200.dp)
    ) {
        content()
    }
}