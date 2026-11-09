package dev.jotalac.feature.notebooks_management.ui.edit_notebook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
            EditNotebookContent(
                state = state,
                onEvent = viewModel::onEvent,
                onSubmit = ::onSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { SaveNotebookButton(state, onClick = ::onSubmit) },
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
