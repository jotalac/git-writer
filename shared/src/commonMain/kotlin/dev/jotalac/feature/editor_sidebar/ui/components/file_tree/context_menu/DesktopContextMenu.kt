package dev.jotalac.feature.editor_sidebar.ui.components.file_tree.context_menu

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
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.ui.SidebarAction

@Composable
fun DesktopContextMenu(
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    menuOffset: DpOffset,
    itemType: FileType,
    itemPath: String,
    onAction: (SidebarAction) -> Unit,
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismissRequest,
        offset = menuOffset,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clip(RoundedCornerShape(6.dp))
            .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f), shape = RoundedCornerShape(6.dp))
            .padding(5.dp)
            .width(200.dp)

    ) {
        if (itemType == FileType.FILE) {
            FileContextMenuContent(
                onAction = onAction,
                itemPath = itemPath,
                onDismissRequest = onDismissRequest
            )
        } else {
            FolderContextMenuContent(
                onAction = onAction,
                itemPath = itemPath,
                onDismissRequest = onDismissRequest
            )
        }
    }
}