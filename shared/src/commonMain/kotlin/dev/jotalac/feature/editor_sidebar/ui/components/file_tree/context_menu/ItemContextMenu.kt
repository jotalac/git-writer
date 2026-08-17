package dev.jotalac.feature.editor_sidebar.ui.components.file_tree.context_menu

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.ContextMenuItem
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.ui.SidebarAction
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AdaptiveContextMenu(
    showMenu: Boolean,
    menuOffset: DpOffset = DpOffset(0.dp, 0.dp),
    itemType: FileType,
    itemPath: String,
    onDismissRequest: () -> Unit,
    onAction: (SidebarAction) -> Unit
) {
    if (isDesktopPlatform) {
        FileTreeContextMenu(
            showMenu = showMenu,
            menuOffset = menuOffset,
            itemType = itemType,
            itemPath = itemPath,
            onAction = onAction,
            onDismissRequest = onDismissRequest
        )
    } else {
        MobileContextMenu(
            showMenu = showMenu,
            itemType = itemType,
            itemPath = itemPath,
            onAction = onAction,
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
fun FileContextMenuContent(
    onAction: (SidebarAction) -> Unit,
    itemPath: String,
    onDismissRequest: () -> Unit,
) {
    ContextMenuItem(
        text = stringResource(Res.string.rename_contect_menu_item),
        iconPainter = Res.drawable.pencil,
        onClick = { onAction(SidebarAction.SetRenameItem(itemPath)) },
        onDismissRequest = onDismissRequest,
    )
    ContextMenuItem(
        text = stringResource(Res.string.delete_item),
        iconPainter = Res.drawable.delete,
        onClick = { onAction(SidebarAction.DeleteItem(itemPath)) },
        onDismissRequest = onDismissRequest,
        itemColor = MaterialTheme.colorScheme.error
    )

    HorizontalDivider()

    ContextMenuItem(
        text = stringResource(Res.string.duplicate_contect_menu_item),
        iconPainter = Res.drawable.duplicate,
        onClick = { onAction(SidebarAction.DuplicateNote(itemPath)) },
        onDismissRequest = onDismissRequest,
    )

    if (isDesktopPlatform) {
        ContextMenuItem(
            text = stringResource(Res.string.copy_contect_menu_item),
            iconPainter = Res.drawable.copy,
            onClick = { onAction(SidebarAction.CopyItemPath(itemPath)) },
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
fun FolderContextMenuContent(
    onAction: (SidebarAction) -> Unit,
    itemPath: String,
    onDismissRequest: () -> Unit,
) {

    ContextMenuItem(
        text = stringResource(Res.string.note_add_contect_menu_item),
        iconPainter = Res.drawable.note_add,
        onClick = { onAction(SidebarAction.AddNote(itemPath)) },
        onDismissRequest = onDismissRequest,
    )

    ContextMenuItem(
        text = stringResource(Res.string.folder_add_contect_menu_item),
        iconPainter = Res.drawable.folder_create,
        onClick = { onAction(SidebarAction.AddFolder(itemPath)) },
        onDismissRequest = onDismissRequest,
    )

    ContextMenuItem(
        text = stringResource(Res.string.rename_contect_menu_item),
        iconPainter = Res.drawable.pencil,
        onClick = { onAction(SidebarAction.SetRenameItem(itemPath)) },
        onDismissRequest = onDismissRequest,
    )

    ContextMenuItem(
        text = stringResource(Res.string.delete_item),
        iconPainter = Res.drawable.delete,
        onClick = { onAction(SidebarAction.DeleteItem(itemPath)) },
        onDismissRequest = onDismissRequest,
        itemColor = MaterialTheme.colorScheme.error
    )

    if (isDesktopPlatform) {
        HorizontalDivider()

        ContextMenuItem(
            text = stringResource(Res.string.copy_contect_menu_item),
            iconPainter = Res.drawable.copy,
            onClick = { onAction(SidebarAction.CopyItemPath(itemPath)) },
            onDismissRequest = onDismissRequest,
        )
    }
}
