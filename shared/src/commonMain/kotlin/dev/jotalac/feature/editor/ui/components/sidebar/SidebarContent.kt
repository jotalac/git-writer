package dev.jotalac.feature.editor.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import git_writer.shared.generated.resources.Res
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import dev.jotalac.feature.notebooks_management.ui.CreateNotebookDialog
import git_writer.shared.generated.resources.add_notebook
import git_writer.shared.generated.resources.folder_create
import git_writer.shared.generated.resources.folder_open
import git_writer.shared.generated.resources.open_notebook
import git_writer.shared.generated.resources.open_settings
import git_writer.shared.generated.resources.settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
private fun SidebarButtonWithTooltip(
    onClick: () -> Unit,
    icon: DrawableResource,
    contentDescription: String,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
            10.dp
        ),
        tooltip = {
            PlainTooltip { Text(contentDescription) }
        },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SidebarTopActions(
    onNotebookOpen: () -> Unit,
    onNotebookCreate: () -> Unit,
    onSettingsOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SidebarButtonWithTooltip(
                onClick = onNotebookOpen,
                icon = Res.drawable.folder_open,
                contentDescription = stringResource(Res.string.open_notebook)
            )

            SidebarButtonWithTooltip(
                onClick = onNotebookCreate,
                icon = Res.drawable.folder_create,
                contentDescription = stringResource(Res.string.add_notebook)
            )

            SidebarButtonWithTooltip(
                onClick = onSettingsOpen,
                icon = Res.drawable.settings,
                contentDescription = stringResource(Res.string.open_settings)
            )
        }
    }
}

@Composable
private fun ActiveNotebookName(
    notebookName: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (notebookName != null) {
            Icon(
                painter = painterResource(Res.drawable.folder_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = notebookName ?: "No active notebook",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (notebookName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
fun SidebarContent(
    modifier: Modifier = Modifier
) {
    val notebookPathProvider = koinInject<NotebookPathProvider>()
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateNotebookDialog(
            onDismiss = { showCreateDialog = false }
        )
    }


    Column (
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        SidebarTopActions(
            onNotebookOpen = {},
            onNotebookCreate = {showCreateDialog = true},
            onSettingsOpen = {},
        )
        Spacer(modifier = Modifier.height(16.dp))

        ActiveNotebookName("Notebook name")

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "No files opened",
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}