package dev.jotalac.feature.editor_sidebar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActiveNotebookMenu(
    notebookName: String?,
    anyFolderExpanded: Boolean,
    onCollapseToggled: () -> Unit,
    onAddNoteClick: () -> Unit,
    onAddFolderClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActiveNotebookName(notebookName)
        Spacer(Modifier.height(12.dp))
        ActiveNotebookActions(
            anyFolderExpanded,
            notebookName == null,
            onCollapseToggled,
            onAddNoteClick,
            onAddFolderClick
        )
    }
}

@Composable
private fun ActiveNotebookName(notebookName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = notebookName ?: stringResource(Res.string.no_active_notebook_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (notebookName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActiveNotebookActions(
    anyFolderExpanded: Boolean,
    isNotebookNull: Boolean,
    onCollapseToggled: () -> Unit,
    onAddNoteClick: () -> Unit,
    onAddFolderClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().offset(x = (-4).dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SidebarButtonWithTooltip(
            onClick = onAddNoteClick,
            icon = Res.drawable.note_add,
            contentDescription = stringResource(Res.string.create_new_file),
            tint = MaterialTheme.colorScheme.secondary,
            enabled = !isNotebookNull
        )
        SidebarButtonWithTooltip(
            onClick = onAddFolderClick,
            icon = Res.drawable.folder_create,
            contentDescription = stringResource(Res.string.create_new_folder),
            tint = MaterialTheme.colorScheme.secondary,
            enabled = !isNotebookNull
        )
        SidebarButtonWithTooltip(
            onClick = onCollapseToggled,
            icon = if (anyFolderExpanded) Res.drawable.collapse_all else Res.drawable.expand_all,
            contentDescription = if (anyFolderExpanded) stringResource(Res.string.collapse_folders) else stringResource(
                Res.string.expand_folders
            ),
            tint = MaterialTheme.colorScheme.secondary,
            enabled = !isNotebookNull
        )
    }
}