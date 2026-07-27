package dev.jotalac.feature.editor_sidebar.ui.components.file_tree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.editor_sidebar.domain.FileType
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.copy
import git_writer.shared.generated.resources.copy_contect_menu_item
import git_writer.shared.generated.resources.delete
import git_writer.shared.generated.resources.delete_contect_menu_item
import git_writer.shared.generated.resources.duplicate
import git_writer.shared.generated.resources.duplicate_contect_menu_item
import git_writer.shared.generated.resources.expand_all
import git_writer.shared.generated.resources.folder_add_contect_menu_item
import git_writer.shared.generated.resources.folder_create
import git_writer.shared.generated.resources.git_merge
import git_writer.shared.generated.resources.note_add
import git_writer.shared.generated.resources.note_add_contect_menu_item
import git_writer.shared.generated.resources.pencil
import git_writer.shared.generated.resources.plus
import git_writer.shared.generated.resources.rename_contect_menu_item
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ItemContextMenu(
    showMenu: Boolean,
    menuOffset: DpOffset,
    itemType: FileType,
    onDismissRequest: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
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

    ) {
        if (itemType == FileType.FILE) {
            FileContextMenuContent(
                onRename,
                onDelete,
                {},
                {},
                onDismissRequest
            )
        } else {
            FolderContextMenuContent(
                onRename,
                onDelete,
                {},
                {},
                {},
                onDismissRequest
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
                        .size(15.dp),
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
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onPathCopy: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    ContextMenuItem(
        text = stringResource(Res.string.rename_contect_menu_item),
        iconPainter = Res.drawable.pencil,
        onClick = onRename,
        onDismissRequest = onDismissRequest,
    )
    ContextMenuItem(
        text = stringResource(Res.string.delete_contect_menu_item),
        iconPainter = Res.drawable.delete,
        onClick = onDelete,
        onDismissRequest = onDismissRequest,
        itemColor = MaterialTheme.colorScheme.error
    )

    HorizontalDivider()

    ContextMenuItem(
        text = stringResource(Res.string.duplicate_contect_menu_item),
        iconPainter = Res.drawable.duplicate,
        onClick = onDuplicate,
        onDismissRequest = onDismissRequest,
    )

    ContextMenuItem(
        text = stringResource(Res.string.copy_contect_menu_item),
        iconPainter = Res.drawable.copy,
        onClick = onPathCopy,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun FolderContextMenuContent(
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddNote: () -> Unit,
    onAddFolder: () -> Unit,
    onPathCopy: () -> Unit,
    onDismissRequest: () -> Unit,
) {

    ContextMenuItem(
        text = stringResource(Res.string.note_add_contect_menu_item),
        iconPainter = Res.drawable.note_add,
        onClick = onAddNote,
        onDismissRequest = onDismissRequest,
    )

    ContextMenuItem(
        text = stringResource(Res.string.folder_add_contect_menu_item),
        iconPainter = Res.drawable.folder_create,
        onClick = onAddFolder,
        onDismissRequest = onDismissRequest,
    )

    ContextMenuItem(
        text = stringResource(Res.string.rename_contect_menu_item),
        iconPainter = Res.drawable.pencil,
        onClick = onRename,
        onDismissRequest = onDismissRequest,
    )

    ContextMenuItem(
        text = stringResource(Res.string.delete_contect_menu_item),
        iconPainter = Res.drawable.delete,
        onClick = onDelete,
        onDismissRequest = onDismissRequest,
        itemColor = MaterialTheme.colorScheme.error
    )

    HorizontalDivider()

    ContextMenuItem(
        text = stringResource(Res.string.copy_contect_menu_item),
        iconPainter = Res.drawable.copy,
        onClick = onPathCopy,
        onDismissRequest = onDismissRequest,
    )
}

