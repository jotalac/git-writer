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
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.ui.components.SubmittableTextField
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
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
            isEncrypted = notebook.isEncrypted,
        )
    }

    fun onSubmit() {
        viewModel.onEvent(EditNotebookViewModel.EditNotebookEvent.SaveNotebook {
            onDismiss()
        })
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
                SubmittableTextField(
                    value = state.notebookName,
                    onValueChange = { viewModel.onEvent(EditNotebookViewModel.EditNotebookEvent.NameChanged(it)) },
                    label = Res.string.notebook_name_placeholder,
                    modifier = Modifier.fillMaxWidth(),
                    submit = { onSubmit() }
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.remote_credentials_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )

                    TextButton(
                        onClick = { viewModel.onEvent(EditNotebookViewModel.EditNotebookEvent.ClearRemoteCredentials) },
                        // an encrypted notebook's bundles are on its remote
                        enabled = !state.isEncrypted,
                    ) {
                        Text(
                            text = stringResource(Res.string.clear_button),
                            maxLines = 1,
                        )
                    }

                }

                OutlinedTextField(
                    value = state.remoteUrl,
                    onValueChange = { viewModel.onEvent(EditNotebookViewModel.EditNotebookEvent.RemoteUrlChanged(it)) },
                    label = { Text(stringResource(Res.string.git_repository_url_input_label)) },
                    placeholder = { Text(stringResource(Res.string.git_remote_example)) },
                    singleLine = true,
                    enabled = !state.isEncrypted,
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

                EncryptionSection(state, onEvent = viewModel::onEvent)

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
                onClick = { onSubmit() },
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

@Composable
private fun EncryptionSection(state: EditNotebookState, onEvent: (EditNotebookViewModel.EditNotebookEvent) -> Unit) {
    HorizontalDivider()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.encrypt_notebook_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    if (state.isEncrypted) Res.string.notebook_encrypted_info else Res.string.encrypt_notebook_subtitle
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // turning it off would need the notes moved to a readable remote again - not supported
        if (!state.isEncrypted) {
            Switch(
                checked = state.encrypt,
                onCheckedChange = { onEvent(EditNotebookViewModel.EditNotebookEvent.EncryptChanged(it)) },
                enabled = !state.isLoading,
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.shield_lock),
                contentDescription = stringResource(Res.string.notebook_encrypted_info),
                modifier = Modifier.size(32.dp)
            )
        }
    }

    if (state.encrypt && !state.isEncrypted) {
        OutlinedTextField(
            value = state.encryptionPassword,
            onValueChange = { onEvent(EditNotebookViewModel.EditNotebookEvent.EncryptionPasswordChanged(it)) },
            label = { Text(stringResource(Res.string.encryption_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.encryptionPasswordConfirm,
            onValueChange = { onEvent(EditNotebookViewModel.EditNotebookEvent.EncryptionPasswordConfirmChanged(it)) },
            label = { Text(stringResource(Res.string.encryption_password_confirm_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            supportingText = { Text(stringResource(Res.string.encryption_password_warning)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
