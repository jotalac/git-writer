package dev.jotalac.feature.sidebar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AdaptiveMenu
import dev.jotalac.core.ui.components.ContextMenuHeader
import dev.jotalac.core.ui.components.ContextMenuItem
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.feature.notebooks_management.domain.Notebook
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The notebook the sidebar shows, at its top - a click opens the menu of all the notebooks: switching to another one,
 * editing the open one, creating one and managing them all.
 */
@Composable
fun NotebookSwitcher(
    activeNotebook: Notebook?,
    notebooks: List<Notebook>,
    onSwitch: (Notebook) -> Unit,
    onCreate: () -> Unit,
    onManage: () -> Unit,
    onEditActive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    // the menu opens right under the switcher
    var height by remember { mutableIntStateOf(0) }
    val menuOffset = with(LocalDensity.current) { DpOffset(0.dp, height.toDp() + 4.dp) }
    val shape = RoundedCornerShape(12.dp)

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { height = it.height }
                .clip(shape)
                .background(
                    if (showMenu) MaterialTheme.colorScheme.surfaceContainerHighest
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
                .clickable { showMenu = true }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NotebookAvatar(
                activeNotebook != null,
                !activeNotebook?.remoteUrl.isNullOrEmpty(),
                activeNotebook?.isEncrypted ?: false
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = activeNotebook?.name ?: stringResource(Res.string.sidebar_no_notebook_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (activeNotebook != null) {
                    NotebookStatus(activeNotebook)
                } else {
                    StatusText(stringResource(Res.string.sidebar_no_notebook_hint))
                }
            }

            Icon(
                painter = painterResource(Res.drawable.unfold_more),
                contentDescription = stringResource(Res.string.notebook_switcher_switch),
                modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AdaptiveMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, offset = menuOffset) {
            NotebookMenuItems(
                activeNotebook = activeNotebook,
                notebooks = notebooks,
                onSwitch = onSwitch,
                onCreate = onCreate,
                onManage = onManage,
                onEditActive = onEditActive,
                onDismissRequest = { showMenu = false },
            )
        }
    }
}

@Composable
private fun NotebookMenuItems(
    activeNotebook: Notebook?,
    notebooks: List<Notebook>,
    onSwitch: (Notebook) -> Unit,
    onCreate: () -> Unit,
    onManage: () -> Unit,
    onEditActive: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (activeNotebook != null) {
        ContextMenuItem(
            text = stringResource(Res.string.edit_active_notebook),
            iconPainter = Res.drawable.edit_pencil,
            onClick = onEditActive,
            onDismissRequest = onDismissRequest,
        )
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 3.dp, vertical = 4.dp),
    )

    if (notebooks.isNotEmpty()) {
        ContextMenuHeader(stringResource(Res.string.sidebar_notebooks_header))
        for (notebook in notebooks) {
            val isActive = notebook.id == activeNotebook?.id
            ContextMenuItem(
                text = notebook.name,
                iconPainter = if (isActive) Res.drawable.check else Res.drawable.opened_book,
                itemColor = if (isActive) MaterialTheme.colorScheme.primary else null,
                onClick = { if (!isActive) onSwitch(notebook) },
                onDismissRequest = onDismissRequest,
            )
        }
   }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 3.dp, vertical = 4.dp),
    )

    ContextMenuItem(
        text = stringResource(Res.string.notebook_menu_new),
        iconPainter = Res.drawable.plus,
        onClick = onCreate,
        onDismissRequest = onDismissRequest,
    )
    ContextMenuItem(
        text = stringResource(Res.string.notebook_menu_manage),
        iconPainter = Res.drawable.format_list_bulleted,
        onClick = onManage,
        onDismissRequest = onDismissRequest,
    )


}

// the first letter of the notebook's name - tells the notebooks apart at a glance
@Composable
private fun NotebookAvatar(
    isNotebookOpened: Boolean,
    isGitSynced: Boolean,
    isE2ee: Boolean
) {
    val size = MaterialTheme.dimensions.buttonCompact
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isNotebookOpened) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest
            ),
        contentAlignment = Alignment.Center,
    ) {
        val icon = when {
            isGitSynced && !isE2ee -> painterResource(Res.drawable.git_fork)
            isGitSynced && isE2ee -> painterResource(Res.drawable.shield_lock)
            else -> painterResource(Res.drawable.devices)
        }
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

// where the notes are kept: synced with git or only on this device, and whether they are encrypted
@Composable
private fun NotebookStatus(notebook: Notebook) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (notebook.isEncrypted) {
            StatusText(stringResource(Res.string.notebook_status_encrypted_msg), modifier = Modifier.weight(1f, fill = false))
        } else if (notebook.remoteUrl != null) {
            StatusText(stringResource(Res.string.notebook_status_synced), modifier = Modifier.weight(1f, fill = false))
        } else {
            StatusText(stringResource(Res.string.notebook_status_local), modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun StatusText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
