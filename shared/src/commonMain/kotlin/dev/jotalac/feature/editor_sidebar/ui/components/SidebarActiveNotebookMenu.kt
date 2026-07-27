package dev.jotalac.feature.editor_sidebar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.collapse_all
import git_writer.shared.generated.resources.collapse_folders
import git_writer.shared.generated.resources.create_new_file
import git_writer.shared.generated.resources.create_new_folder
import git_writer.shared.generated.resources.expand_all
import git_writer.shared.generated.resources.expand_folders
import git_writer.shared.generated.resources.folder_create
import git_writer.shared.generated.resources.no_active_notebook_label
import git_writer.shared.generated.resources.note_add
import git_writer.shared.generated.resources.opened_book
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

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
        ActiveNotebookActions(anyFolderExpanded, onCollapseToggled, onAddNoteClick, onAddFolderClick)
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
            tint = MaterialTheme.colorScheme.secondary

        )
        SidebarButtonWithTooltip(
            onClick = onAddFolderClick,
            icon = Res.drawable.folder_create,
            contentDescription = stringResource(Res.string.create_new_folder),
            tint = MaterialTheme.colorScheme.secondary
        )
        SidebarButtonWithTooltip(
            onClick = onCollapseToggled,
            icon = if (anyFolderExpanded) Res.drawable.collapse_all else Res.drawable.expand_all,
            contentDescription = if (anyFolderExpanded) stringResource(Res.string.collapse_folders) else stringResource(Res.string.expand_folders),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}