package dev.jotalac.feature.editor_sidebar.ui.components.file_tree.context_menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.ui.SidebarAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileContextMenu(
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    itemType: FileType,
    itemPath: String,
    onAction: (SidebarAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 24.dp)
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
}