package dev.jotalac.feature.sidebar.ui.components.file_tree.context_menu

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.ContextMenuItem
import dev.jotalac.core.ui.components.DesktopContextMenu
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.sidebar.domain.FileType
import dev.jotalac.feature.sidebar.ui.SidebarAction
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// context menu for item in file tree
@Composable
fun AdaptiveContextMenu(
    showMenu: Boolean,
    menuOffset: DpOffset = DpOffset(0.dp, 0.dp),
    itemType: FileType,
    itemPath: String,
    onDismissRequest: () -> Unit,
    onAction: (SidebarAction) -> Unit,
    isRoot: Boolean = false
) {
    val items: @Composable () -> Unit = {
        when (itemType) {
            FileType.DIRECTORY -> DirectoryContextMenuContent(onAction, itemPath, onDismissRequest, isRoot)
            FileType.NOTE -> FileContextMenuContent(onAction, itemPath, onDismissRequest, isNote = true)
            FileType.GENERAL_FILE -> FileContextMenuContent(onAction, itemPath, onDismissRequest, isNote = false)
        }
    }

    if (isDesktopPlatform) {
        DesktopContextMenu(expanded = showMenu, onDismissRequest = onDismissRequest, offset = menuOffset, content = items)
    } else {
        MobileContextMenu(showMenu = showMenu, onDismissRequest = onDismissRequest, content = items)
    }
}

// a note can also be duplicated and exported, other files (images, pdfs) are not text
@Composable
private fun FileContextMenuContent(
    onAction: (SidebarAction) -> Unit,
    itemPath: String,
    onDismissRequest: () -> Unit,
    isNote: Boolean,
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

    if (isNote || isDesktopPlatform) HorizontalDivider()

    if (isNote) {
        ContextMenuItem(
            text = stringResource(Res.string.duplicate_contect_menu_item),
            iconPainter = Res.drawable.duplicate,
            onClick = { onAction(SidebarAction.DuplicateNote(itemPath)) },
            onDismissRequest = onDismissRequest,
        )

        ContextMenuItem(
            text = stringResource(Res.string.export_pdf_context_menu_item),
            iconPainter = Res.drawable.picture_as_pdf,
            onClick = { onAction(SidebarAction.ExportPdf(itemPath)) },
            onDismissRequest = onDismissRequest,
        )
    }

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
private fun DirectoryContextMenuContent(
    onAction: (SidebarAction) -> Unit,
    itemPath: String,
    onDismissRequest: () -> Unit,
    isRoot: Boolean = false
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

    if (!isRoot) {
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
    }

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
