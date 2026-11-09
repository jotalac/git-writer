package dev.jotalac.feature.notebooks_management.ui.edit_notebook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.ui.components.CustomScaffold
import dev.jotalac.core.ui.components.TopAppBarIcon
import dev.jotalac.core.ui.window.TitleBar
import dev.jotalac.core.ui.window.WindowTitle
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Editing a notebook on mobile - [EditNotebookDialog] on desktop. */
@Composable
fun EditNotebookScreen(
    notebookId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditNotebookViewModel = koinViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = stringResource(Res.string.edit_notebook_title)

    WindowTitle(title)

    LaunchedEffect(notebookId) {
        viewModel.loadNotebook(notebookId)
    }

    fun onSubmit() {
        viewModel.onEvent(EditNotebookViewModel.EditNotebookEvent.SaveNotebook(onSuccess = onNavigateBack))
    }

    CustomScaffold(
        snackbarHostState = snackbarHostState,
        topAppBar = {
            TitleBar(
                leading = {
                    TopAppBarIcon(
                        onClick = onNavigateBack,
                        icon = Res.drawable.arrow_right,
                        contentDescription = stringResource(Res.string.navigate_back),
                        modifier = Modifier.rotate(180f),
                    )
                },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(Res.drawable.edit_pencil), contentDescription = null)
                        Text(text = title)
                    }
                },
                actions = {
                    SaveNotebookButton(state, onClick = ::onSubmit, modifier = Modifier.padding(end = 8.dp))
                },
            )
        }
    ) { innerPadding ->
        EditNotebookContent(
            state = state,
            onEvent = viewModel::onEvent,
            onSubmit = ::onSubmit,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        )
    }
}
