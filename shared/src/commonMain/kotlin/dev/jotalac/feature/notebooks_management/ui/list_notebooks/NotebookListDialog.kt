package dev.jotalac.feature.notebooks_management.ui.list_notebooks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AlertDialogTitleWithIcon
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.feature.notebooks_management.domain.Notebook
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotebookListDialog(
    onDismiss: () -> Unit,
    activeNotebookId: Long? = null,
    viewModel: NotebookListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            AlertDialogTitleWithIcon(
                iconResource = Res.drawable.format_list_bulleted,
                text = Res.string.open_notebook_title,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.notebooks.isEmpty()) {
                    Text(
                        stringResource(Res.string.no_saved_notebooks_msg),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(min = 200.dp, max = 400.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.notebooks) { notebook ->
                                NotebookListItem(
                                    isActive = activeNotebookId == notebook.id,
                                    notebook = notebook,
                                    onItemClick = {
                                        viewModel.onEvent(NotebookListViewModel.NotebookListEvent.OpenNotebook(notebook.id) {
                                            onDismiss()
                                        })
                                    },
                                    onDeleteClick = {
                                        viewModel.onEvent(
                                            NotebookListViewModel.NotebookListEvent.DeleteNotebook(
                                                notebook.id
                                            )
                                        )
                                    }
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

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel_button))
            }
        }
    )
}

@Composable
private fun NotebookListItem(
    isActive: Boolean,
    notebook: Notebook,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable {
                onItemClick()
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = notebook.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notebook.directoryPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { onDeleteClick() }
        ) {
            Icon(
                painter = painterResource(Res.drawable.delete),
                contentDescription = stringResource(Res.string.delete_icon),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(MaterialTheme.dimensions.iconMedium)
            )
        }
    }
}