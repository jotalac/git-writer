package dev.jotalac.feature.git_sync.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AlertDialogTitleWithIcon
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.dimensions
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = 280.dp, max = 560.dp),
        title = {
            AlertDialogTitleWithIcon(
                iconResource = Res.drawable.git_merge_conflict,
                text = Res.string.git_merge_conflict,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.git_conflict_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.git_conflict_keep_files_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = "${conflictedFileNames.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
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
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
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
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(Res.string.abort_sync_button),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
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
        FilledTonalButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(text),
                modifier = Modifier.size(MaterialTheme.dimensions.iconSmall)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                style = MaterialTheme.typography.bodyMedium,
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
                modifier = Modifier.size(MaterialTheme.dimensions.iconMedium)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.labelLarge,
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