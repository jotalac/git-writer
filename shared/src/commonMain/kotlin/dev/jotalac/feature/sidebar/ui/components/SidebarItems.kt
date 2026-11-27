package dev.jotalac.feature.sidebar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jotalac.core.ui.components.DelayedTooltip
import dev.jotalac.core.ui.theme.dimensions
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// full width item in sidebar - eg. settings button
@Composable
fun SidebarNavItem(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = MaterialTheme.dimensions.listItemPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.dimensions.listItemTextSize),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** The title of a part of the sidebar ("Files"), with its small [actions] at the end. */
@Composable
fun SidebarSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** What is done most in a notebook: a new note - labelled, so it is found - and a new folder next to it. */
@Composable
fun SidebarPrimaryActions(
    onNewNote: () -> Unit,
    onNewFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = MaterialTheme.dimensions.buttonStandard
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onNewNote,
            modifier = Modifier.weight(1f).height(height),
            shape = shape,
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.note_add),
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.sidebar_new_note),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val newFolder = stringResource(Res.string.sidebar_new_folder)
        DelayedTooltip(text = newFolder) {
            OutlinedIconButton(
                onClick = onNewFolder,
                modifier = Modifier.size(height),
                shape = shape,
                border = ButtonDefaults.outlinedButtonBorder(),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.folder_create),
                    contentDescription = newFolder,
                    modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The sidebar without a notebook - what a notebook is, and how to get one. */
@Composable
fun NoNotebookOpen(
    hasNotebooks: Boolean,
    onCreate: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.opened_book),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = stringResource(Res.string.sidebar_no_notebook_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
            Icon(
                painter = painterResource(Res.drawable.plus),
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.notebook_menu_new))
        }
        if (hasNotebooks) {
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text(stringResource(Res.string.open_notebook))
            }
        }
    }
}
