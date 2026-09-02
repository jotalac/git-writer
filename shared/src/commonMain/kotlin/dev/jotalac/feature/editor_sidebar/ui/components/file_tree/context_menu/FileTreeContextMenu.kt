package dev.jotalac.feature.editor_sidebar.ui.components.file_tree.context_menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import dev.jotalac.core.ui.components.DesktopContextMenu
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.ui.SidebarAction

@Composable
fun FileTreeContextMenu(
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    menuOffset: DpOffset,
    itemType: FileType,
    itemPath: String,
    onAction: (SidebarAction) -> Unit,
    isRoot: Boolean,
) {
    DesktopContextMenu(
        expanded = showMenu,
        onDismissRequest = onDismissRequest,
        offset = menuOffset
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
                onDismissRequest = onDismissRequest,
                isRoot = isRoot
            )
        }
    }
}