package dev.jotalac.feature.notebooks_management.ui.edit_notebook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AlertDialogTitleWithIcon
import dev.jotalac.feature.notebooks_management.domain.Notebook
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditNotebookDialog(
    notebook: Notebook,
    onDismiss: () -> Unit,
    viewModel: EditNotebookViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initWithNotebook(
            id = notebook.id,
            name = notebook.name,
            remoteUrl = notebook.remoteUrl,
            remoteUsername = notebook.remoteUsername,
            remotePassword = notebook.remotePassword,
        )
    }

    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = {
            AlertDialogTitleWithIcon(
                iconResource = Res.drawable.edit_pencil,
                text = Res.string.edit_notebook_title,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = state.notebookName,
                    onValueChange = { viewModel.onEvent(EditNotebookViewModel.EditNotebookEvent.NameChanged(it)) },
                    label = { Text(stringResource(Res.string.notebook_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = stringResource(Res.string.remote_credentials_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = state.remoteUrl,
                    onValueChange = { viewModel.onEvent(EditNotebookViewModel.EditNotebookEvent.RemoteUrlChanged(it)) },
                    label = { Text(stringResource(Res.string.git_repository_url_input_label)) },
                    placeholder = { Text(stringResource(Res.string.git_remote_example)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth > 400.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = state.remoteUsername,
                                onValueChange = {
                                    viewModel.onEvent(
                                        EditNotebookViewModel.EditNotebookEvent.UsernameChanged(
                                            it
                                        )
                                    )
                                },
                                label = { Text(stringResource(Res.string.username_placeholder)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state.remotePassword,
                                onValueChange = {
                                    viewModel.onEvent(
                                        EditNotebookViewModel.EditNotebookEvent.PasswordChanged(
                                            it
                                        )
                                    )
                                },
                                label = { Text(stringResource(Res.string.git_auth_placeholder)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = state.remoteUsername,
                                onValueChange = {
                                    viewModel.onEvent(
                                        EditNotebookViewModel.EditNotebookEvent.UsernameChanged(
                                            it
                                        )
                                    )
                                },
                                label = { Text(stringResource(Res.string.username_placeholder)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.remotePassword,
                                onValueChange = {
                                    viewModel.onEvent(
                                        EditNotebookViewModel.EditNotebookEvent.PasswordChanged(
                                            it
                                        )
                                    )
                                },
                                label = { Text(stringResource(Res.string.git_auth_placeholder)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.onEvent(EditNotebookViewModel.EditNotebookEvent.SaveNotebook {
                        onDismiss()
                    })
                },
                enabled = state.notebookName.isNotBlank() && !state.isLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(stringResource(Res.string.save_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isLoading
            ) {
                Text(stringResource(Res.string.cancel_button))
            }
        }
    )
}
