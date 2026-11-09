package dev.jotalac.feature.sidebar.ui.components.file_tree.context_menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.design.BottomSheet
import dev.jotalac.feature.sidebar.domain.FileType
import dev.jotalac.feature.sidebar.ui.SidebarAction

/** A file's or a folder's actions in a sheet from the bottom - the right click menu of mobile. */
@Composable
fun MobileContextMenu(
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    itemType: FileType,
    itemPath: String,
    onAction: (SidebarAction) -> Unit
) {
    if (!showMenu) return

    BottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
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
}
