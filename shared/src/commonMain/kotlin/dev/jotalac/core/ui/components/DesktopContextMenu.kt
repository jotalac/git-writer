package dev.jotalac.core.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.design.DropdownMenu

/** A right click menu, [offset] from its anchor's bottom left corner - its items are [ContextMenuItem]s. */
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
        modifier = Modifier.width(220.dp),
    ) {
        DisableSelection {
            content()
        }
    }
}
