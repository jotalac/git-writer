package dev.jotalac.feature.editor.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.closed_sidebar
import git_writer.shared.generated.resources.opened_sidebar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import androidx.compose.ui.text.font.FontWeight
import git_writer.shared.generated.resources.folder_create
import git_writer.shared.generated.resources.folder_open
import git_writer.shared.generated.resources.settings
import git_writer.shared.generated.resources.sidebar_title
import org.jetbrains.compose.resources.stringResource

@Composable
private fun SidebarActionButton(
    onClick: () -> Unit,
    icon: DrawableResource,
    contentDescription: String,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun SidebarTopActions() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.sidebar_title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SidebarActionButton(
                onClick = {},
                icon = Res.drawable.folder_open,
                contentDescription = "Open folder"
            )

            SidebarActionButton(
                onClick = {},
                icon = Res.drawable.folder_create,
                contentDescription = "Create folder"
            )

            SidebarActionButton(
                onClick = {},
                icon = Res.drawable.settings,
                contentDescription = "Open settings"
            )
        }
    }
}


@Composable
fun SidebarContent(
    modifier: Modifier = Modifier
) {
    Column (
        modifier = modifier
            .fillMaxHeight(),
    ) {
        SidebarTopActions()
        
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