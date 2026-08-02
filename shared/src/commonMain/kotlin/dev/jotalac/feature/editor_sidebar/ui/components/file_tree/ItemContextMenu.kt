package dev.jotalac.feature.editor_sidebar.ui.components.file_tree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.ui.SidebarAction
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ItemContextMenu(
    showMenu: Boolean,
    menuOffset: DpOffset,
    itemType: FileType,
    itemPath: String,
    onDismissRequest: () -> Unit,
    onAction: (SidebarAction) -> Unit
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

@Composable
private fun ContextMenuItem(
    text: String,
    iconPainter: DrawableResource,
    onClick: () -> Unit,
    onDismissRequest: () -> Unit,
    itemColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier.clip(RoundedCornerShape(6.dp))
            ) {
                Icon(
                    painter = painterResource(iconPainter),
                    contentDescription = text,
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.iconMedium),
                    tint = itemColor ?: MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = itemColor ?: MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        onClick = {
            onDismissRequest()
            onClick()
        },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .padding(3.dp)
    )
}

@Composable
private fun FileContextMenuContent(
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
        text = stringResource(Res.string.delete_contect_menu_item),
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
private fun FolderContextMenuContent(
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
        text = stringResource(Res.string.delete_contect_menu_item),
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
