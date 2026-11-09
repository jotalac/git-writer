package dev.jotalac.feature.notebooks_management.ui.edit_notebook

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.jotalac.core.ui.design.AppDialog
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.design.TextButton
import dev.jotalac.feature.notebooks_management.domain.Notebook
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

    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.edit_notebook_title),
        icon = painterResource(Res.drawable.edit_pencil),
        dismissible = !state.isLoading,
        scrollContent = true,
        actions = {
            TextButton(onClick = onDismiss, enabled = !state.isLoading) {
                Text(stringResource(Res.string.cancel_button))
            }
            SaveNotebookButton(state, onClick = ::onSubmit)
        },
    ) {
        EditNotebookContent(
            state = state,
            onEvent = viewModel::onEvent,
            onSubmit = ::onSubmit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
