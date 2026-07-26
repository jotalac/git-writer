package dev.jotalac.feature.editor_sidebar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.add_diamond
import git_writer.shared.generated.resources.add_notebook
import git_writer.shared.generated.resources.folder_open
import git_writer.shared.generated.resources.open_notebook
import git_writer.shared.generated.resources.open_settings
import git_writer.shared.generated.resources.settings
import org.jetbrains.compose.resources.stringResource

@Composable
fun SidebarGlobalActions(
    onNotebookOpen: () -> Unit,
    onNotebookCreate: () -> Unit,
    onSettingsOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
//            .background(
////                color = MaterialTheme.colorScheme.surfaceContainerHighest,
////                shape = RoundedCornerShape(8.dp)
//
//            )
            .padding(horizontal = 12.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SidebarButtonWithTooltip(
            onClick = onNotebookOpen,
            icon = Res.drawable.folder_open,
            contentDescription = stringResource(Res.string.open_notebook)
        )

        SidebarButtonWithTooltip(
            onClick = onNotebookCreate,
            icon = Res.drawable.add_diamond,
            contentDescription = stringResource(Res.string.add_notebook)
        )

        SidebarButtonWithTooltip(
            onClick = onSettingsOpen,
            icon = Res.drawable.settings,
            contentDescription = stringResource(Res.string.open_settings)
        )
    }
}

