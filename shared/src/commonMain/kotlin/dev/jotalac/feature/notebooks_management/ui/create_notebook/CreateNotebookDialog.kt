package dev.jotalac.feature.notebooks_management.ui.create_notebook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AlertDialogTitleWithIcon
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.core.utils.toSafeFileName
import dev.jotalac.feature.notebooks_management.ui.validateRemoteUrl
import git_writer.shared.generated.resources.*
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateNotebookDialog(
    onDismiss: () -> Unit,
    viewModel: CreateNotebookViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()

    val actualDirectory by remember(state.notebookName, state.selectedDirectory) {
        derivedStateOf {
            if (state.selectedDirectory != null) {
                "${state.selectedDirectory}/${state.notebookName.toSafeFileName()}"
            } else {
                "${state.defaultBasePath}/${state.notebookName.toSafeFileName()}"
            }
        }
    }

    fun browseForDirectory() {
        scope.launch {
            val directory = FileKit.openDirectoryPicker(
                directory = PlatformFile(if (state.selectedDirectory != null) state.selectedDirectory!! else state.defaultBasePath)
            )

            if (directory != null) {
                viewModel.onEvent(CreateNotebookViewModel.CreateNotebookEvent.DirectorySelected(directory.absolutePath()))
            }
        }
    }

    fun validateCloneForm(): String? {
        return if (state.notebookName.isBlank() || state.remoteUrl.isBlank() ||
            actualDirectory.isBlank() || state.username.isBlank() ||
            state.password.isBlank()
        ) {
            "Please fill in all fields"
        } else {
            validateRemoteUrl(state.remoteUrl)
        }
    }




    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = {
            AlertDialogTitleWithIcon(
                iconResource = Res.drawable.plus,
                text = Res.string.create_notebook_title,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = state.selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    divider = @Composable { HorizontalDivider() }
                ) {
                    Tab(
                        selected = state.selectedTabIndex == 0,
                        onClick = { viewModel.onEvent(CreateNotebookViewModel.CreateNotebookEvent.TabSelected(0)) },
                        text = {
                            TabText(
                                text = Res.string.init_notebook,
                                icon = Res.drawable.plus,
                                contentDescription = Res.string.plus_icon
                            )
                        },
                    )
                    Tab(
                        selected = state.selectedTabIndex == 1,
                        onClick = { viewModel.onEvent(CreateNotebookViewModel.CreateNotebookEvent.TabSelected(1)) },
                        text = {
                            TabText(
                                text = Res.string.clone_notebook,
                                icon = Res.drawable.git_merge,
                                contentDescription = Res.string.git_merge_icon
                            )
                        }
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (state.selectedTabIndex) {
                        0 -> LocalNotebookForm(
                            name = state.notebookName,
                            onNameChange = {
                                viewModel.onEvent(
                                    CreateNotebookViewModel.CreateNotebookEvent.NotebookNameChanged(
                                        it
                                    )
                                )
                            },
                            directory = actualDirectory,
                            onBrowseClick = { browseForDirectory() }
                        )

                        1 -> CloneNotebookForm(
                            name = state.notebookName,
                            onNameChange = {
                                viewModel.onEvent(
                                    CreateNotebookViewModel.CreateNotebookEvent.NotebookNameChanged(
                                        it
                                    )
                                )
                            },
                            url = state.remoteUrl,
                            onUrlChange = {
                                viewModel.onEvent(
                                    CreateNotebookViewModel.CreateNotebookEvent.RemoteUrlChanged(
                                        it
                                    )
                                )
                            },
                            username = state.username,
                            onUsernameChange = {
                                viewModel.onEvent(
                                    CreateNotebookViewModel.CreateNotebookEvent.UsernameChanged(
                                        it
                                    )
                                )
                            },
                            password = state.password,
                            onPasswordChange = {
                                viewModel.onEvent(
                                    CreateNotebookViewModel.CreateNotebookEvent.PasswordChanged(
                                        it
                                    )
                                )
                            },
                            directory = actualDirectory,
                            onBrowseClick = { browseForDirectory() }
                        )
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
                    if (state.selectedTabIndex == 0) {
                        viewModel.onEvent(
                            CreateNotebookViewModel.CreateNotebookEvent.CreateLocalNotebook(
                                actualDirectory
                            ) {
                                onDismiss()
                            })
                    } else {
                        val validationResult = validateCloneForm()
                        if (validationResult != null) {
                            viewModel.onEvent(
                                CreateNotebookViewModel.CreateNotebookEvent.AddErrorMessage(
                                    validationResult
                                )
                            )
                        } else {
                            viewModel.onEvent(
                                CreateNotebookViewModel.CreateNotebookEvent.CloneRemoteNotebook(
                                    actualDirectory
                                ) {
                                    onDismiss()
                                })
                        }
                    }
                },
                enabled = if (state.selectedTabIndex == 0) {
                    state.notebookName.isNotBlank() && actualDirectory.isNotBlank() && !state.isLoading
                } else {
                    state.notebookName.isNotBlank() && state.remoteUrl.isNotBlank() &&
                            actualDirectory.isNotBlank() && state.username.isNotBlank() &&
                            state.password.isNotBlank() && !state.isLoading
                }
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
                    Text(
                        if (state.selectedTabIndex == 0) stringResource(Res.string.create_notebook_title) else stringResource(
                            Res.string.clone_notebook
                        )
                    )
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
private fun TabText(
    text: StringResource,
    icon: DrawableResource,
    contentDescription: StringResource
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(contentDescription),
            modifier = Modifier.size(MaterialTheme.dimensions.iconMedium)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(text))
    }
}

@Composable
private fun LocalNotebookForm(
    name: String,
    onNameChange: (String) -> Unit,
    directory: String?,
    onBrowseClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.notebook_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (isDesktopPlatform) {
            DirectoryPickerRow(
                directory = directory,
                onBrowseClick = onBrowseClick
            )
        }
    }
}

@Composable
private fun CloneNotebookForm(
    name: String,
    onNameChange: (String) -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    directory: String?,
    onBrowseClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.notebook_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text(stringResource(Res.string.git_repository_url_input_label)) },
            placeholder = { Text(stringResource(Res.string.git_remote_example)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ResponsiveRow(modifier = Modifier.fillMaxWidth()) { childModifier ->
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(Res.string.username_placeholder)) },
                singleLine = true,
                modifier = childModifier
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(Res.string.git_auth_placeholder)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = childModifier
            )
        }
        if (isDesktopPlatform) {
            DirectoryPickerRow(
                directory = directory,
                onBrowseClick = onBrowseClick
            )
        }
    }
}

@Composable
fun DirectoryPickerRow(
    directory: String?,
    onBrowseClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.destination_directory_label),
            style = MaterialTheme.typography.labelMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (!directory.isNullOrBlank()) directory else "No directory selected",
                style = MaterialTheme.typography.bodySmall,
                color = if (directory.isNullOrBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            )

            TextButton(onClick = onBrowseClick) {
                Text(stringResource(Res.string.browse_destination_button))
            }
        }
    }
}


@Composable
private fun ResponsiveRow(
    modifier: Modifier = Modifier,
    content: @Composable (modifier: Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth > 400.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content(Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content(Modifier.fillMaxWidth())
            }
        }
    }
}