package dev.jotalac.feature.notebooks_management.ui.create_notebook

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.design.AppDialog
import dev.jotalac.core.ui.design.Button
import dev.jotalac.core.ui.design.SegmentedControl
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.design.TextButton
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.core.utils.toSafeFileName
import dev.jotalac.feature.notebooks_management.ui.components.SubmittableTextField
import dev.jotalac.feature.notebooks_management.ui.validateRemoteUrl
import git_writer.shared.generated.resources.*
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class Tab { Init, Clone, Open }

private val visibleTabs: List<Tab> =
    if (isDesktopPlatform) listOf(Tab.Init, Tab.Clone, Tab.Open)
    else listOf(Tab.Init, Tab.Clone)

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

    // index of a tab in the row currently rendered
    fun tabIndex(tab: Tab): Int = visibleTabs.indexOf(tab)

    fun submitForm() {
        when (state.selectedTabIndex) {
            tabIndex(Tab.Init) -> viewModel.onEvent(
                CreateNotebookViewModel.CreateNotebookEvent.CreateLocalNotebook(actualDirectory) { onDismiss() }
            )

            tabIndex(Tab.Clone) -> validateCloneForm()?.let { message ->
                viewModel.onEvent(CreateNotebookViewModel.CreateNotebookEvent.AddErrorMessage(message))
            } ?: viewModel.onEvent(
                CreateNotebookViewModel.CreateNotebookEvent.CloneRemoteNotebook(actualDirectory) { onDismiss() }
            )

            else -> viewModel.onEvent(
                CreateNotebookViewModel.CreateNotebookEvent.OpenExistingNotebook(onDismiss)
            )
        }
    }



    val selectedTab = visibleTabs.getOrElse(state.selectedTabIndex) { Tab.Init }
    val canSubmit = when (selectedTab) {
        Tab.Init -> state.notebookName.isNotBlank() && actualDirectory.isNotBlank()
        Tab.Open -> !state.selectedDirectory.isNullOrEmpty()
        Tab.Clone -> state.notebookName.isNotBlank() && state.remoteUrl.isNotBlank() &&
            actualDirectory.isNotBlank() && state.username.isNotBlank() && state.password.isNotBlank()
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.create_notebook_title),
        icon = painterResource(Res.drawable.plus),
        dismissible = !state.isLoading,
        scrollContent = true,
        actions = {
            TextButton(onClick = onDismiss, enabled = !state.isLoading) {
                Text(stringResource(Res.string.cancel_button))
            }
            Button(onClick = { submitForm() }, enabled = canSubmit, isLoading = state.isLoading) {
                Text(
                    stringResource(
                        when (selectedTab) {
                            Tab.Init -> Res.string.create_notebook_title
                            Tab.Open -> Res.string.open_notebook
                            Tab.Clone -> Res.string.clone_notebook
                        }
                    )
                )
            }
        },
    ) {
        SegmentedControl(
            options = visibleTabs,
            selected = selectedTab,
            onSelect = { tab -> viewModel.onEvent(CreateNotebookViewModel.CreateNotebookEvent.TabSelected(tabIndex(tab))) },
            label = { tab ->
                stringResource(
                    when (tab) {
                        Tab.Init -> Res.string.init_notebook
                        Tab.Clone -> Res.string.clone_notebook
                        Tab.Open -> Res.string.open_existing_notebook
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            when (state.selectedTabIndex) {
                tabIndex(Tab.Init) -> LocalNotebookForm(
                    name = state.notebookName,
                    onNameChange = {
                        viewModel.onEvent(
                            CreateNotebookViewModel.CreateNotebookEvent.NotebookNameChanged(
                                it
                            )
                        )
                    },
                    directory = actualDirectory,
                    onBrowseClick = { browseForDirectory() },
                    onSubmit = { submitForm() }
                )
                tabIndex(Tab.Open) -> OpenNotebookForm(
                    directory = state.selectedDirectory,
                    onBrowseClick = { browseForDirectory() }
                )

                tabIndex(Tab.Clone) -> CloneNotebookForm(
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
                    encryptionPassword = state.encryptionPassword,
                    onEncryptionPasswordChange = {
                        viewModel.onEvent(CreateNotebookViewModel.CreateNotebookEvent.EncryptionPasswordChanged(it))
                    },
                    directory = actualDirectory,
                    onBrowseClick = { browseForDirectory() },
                    submitForm = { submitForm() }
                )
            }
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = AppTheme.colors.danger,
                style = AppTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LocalNotebookForm(
    name: String,
    onNameChange: (String) -> Unit,
    directory: String?,
    onBrowseClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubmittableTextField(
            value = name,
            onValueChange = onNameChange,
            label = Res.string.notebook_name_placeholder,
            modifier = Modifier.fillMaxWidth(),
            submit = onSubmit
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
private fun OpenNotebookForm(
    directory: String?,
    onBrowseClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DirectoryPickerRow(
                directory = directory,
                onBrowseClick = onBrowseClick
            )
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
    encryptionPassword: String,
    onEncryptionPasswordChange: (String) -> Unit,
    directory: String?,
    onBrowseClick: () -> Unit,
    submitForm: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubmittableTextField(
            value = name,
            onValueChange = onNameChange,
            label = Res.string.notebook_name_placeholder,
            modifier = Modifier.fillMaxWidth(),
            submit = submitForm
        )

        SubmittableTextField(
            value = url,
            onValueChange = onUrlChange,
            label = Res.string.git_repository_url_input_label,
            placeholder = Res.string.git_remote_example,
            modifier = Modifier.fillMaxWidth(),
            submit = submitForm
        )

        ResponsiveRow(modifier = Modifier.fillMaxWidth()) { childModifier ->
            SubmittableTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = Res.string.username_placeholder,
                modifier = childModifier,
                submit = submitForm
            )
            SubmittableTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = Res.string.git_auth_placeholder,
                visualTransformation = PasswordVisualTransformation(),
                modifier = childModifier,
                submit = submitForm
            )
        }
        // only a remote that is an encrypted notebook needs it
        SubmittableTextField(
            value = encryptionPassword,
            onValueChange = onEncryptionPasswordChange,
            label = Res.string.encrypted_notebook_password_label,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            submit = submitForm
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
fun DirectoryPickerRow(
    directory: String?,
    onBrowseClick: () -> Unit,
    isCreating: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (isCreating) stringResource(Res.string.destination_directory_label) else stringResource(Res.string.open_notebook_directory),
            style = AppTheme.typography.labelSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (!directory.isNullOrBlank()) directory else "No directory selected",
                style = AppTheme.typography.bodySmall,
                color = if (directory.isNullOrBlank()) AppTheme.colors.danger else AppTheme.colors.textSecondary,
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
