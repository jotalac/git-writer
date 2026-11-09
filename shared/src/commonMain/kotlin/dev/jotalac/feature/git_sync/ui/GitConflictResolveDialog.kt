package dev.jotalac.feature.git_sync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.core.ui.design.AppDialog
import dev.jotalac.core.ui.design.Button
import dev.jotalac.core.ui.design.ButtonStyle
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.Surface
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.design.TextButton
import dev.jotalac.core.ui.theme.AppTheme
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GitConflictResolveDialog(
    conflictedFileNames: List<String>,
    onKeepLocal: (fileName: String) -> Unit,
    onKeepRemote: (fileName: String) -> Unit,
    onKeepAllLocal: () -> Unit,
    onKeepAllRemote: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    AppDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(Res.string.git_merge_conflict),
        icon = painterResource(Res.drawable.git_merge_conflict),
        actions = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.abort_sync_button),
                    color = AppTheme.colors.danger
                )
            }
        },
    ) {
        Text(
            text = stringResource(Res.string.git_conflict_dialog_description),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.git_conflict_keep_files_label),
                style = AppTheme.typography.heading,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.text
            )

            Surface(
                shape = AppTheme.shapes.small,
                color = AppTheme.colors.dangerSoft,
            ) {
                Text(
                    text = "${conflictedFileNames.size}",
                    style = AppTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.onDangerSoft,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 320.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conflictedFileNames, key = { it }) { fileName ->
                    ConflictedFileItem(
                        fileName = fileName,
                        onKeepLocal = { onKeepLocal(fileName) },
                        onKeepRemote = { onKeepRemote(fileName) }
                    )
                }
            }

            AppVerticalScrollbar(
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentWidth(Alignment.End),
                listState = listState
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth > 340.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConfirmAllButton(
                        text = Res.string.git_merge_all_local,
                        icon = Res.drawable.devices,
                        onClick = onKeepAllLocal,
                        modifier = Modifier.weight(1f)
                    )

                    ConfirmAllButton(
                        text = Res.string.git_merge_all_remote,
                        icon = Res.drawable.cloud,
                        onClick = onKeepAllRemote,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfirmAllButton(
                        text = Res.string.git_merge_all_local,
                        icon = Res.drawable.devices,
                        onClick = onKeepAllLocal,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ConfirmAllButton(
                        text = Res.string.git_merge_all_remote,
                        icon = Res.drawable.cloud,
                        onClick = onKeepAllRemote,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictedFileItem(
    fileName: String,
    onKeepLocal: () -> Unit,
    onKeepRemote: () -> Unit,
    modifier: Modifier = Modifier,
) {

    @Composable
    fun ItemButton(
        onClick: () -> Unit,
        text: StringResource,
        icon: DrawableResource,
    ) {
        Button(
            onClick = onClick,
            style = ButtonStyle.Secondary,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(text),
                modifier = Modifier.size(AppTheme.dimensions.iconSmall)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(text),
                style = AppTheme.typography.labelSmall
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.medium,
        color = AppTheme.colors.surfaceSunken,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = fileName,
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.body,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemButton(
                    onClick = onKeepLocal,
                    text = Res.string.git_merge_local,
                    icon = Res.drawable.devices
                )

                ItemButton(
                    onClick = onKeepRemote,
                    text = Res.string.git_merge_remote,
                    icon = Res.drawable.cloud
                )
            }
        }
    }
}

@Composable
private fun ConfirmAllButton(
    text: StringResource,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(text),
                modifier = Modifier.size(AppTheme.dimensions.iconMedium)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(text),
                style = AppTheme.typography.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
private fun GitConflictResolveDialogPreview() {
    AppTheme {
        GitConflictResolveDialog(
            conflictedFileNames = listOf(
                "notes/daily-journal.md",
                "projects/architecture-design-2026.md",
                "ideas/long_document_title_with_deep_nested_structure.md",
                "todo.md",
                "test.md",
                "yoyo.md",
                "recipes/pasta.md",
            ),
            onKeepLocal = {},
            onKeepRemote = {},
            onKeepAllLocal = {},
            onKeepAllRemote = {},
            onDismiss = {}
        )
    }
}