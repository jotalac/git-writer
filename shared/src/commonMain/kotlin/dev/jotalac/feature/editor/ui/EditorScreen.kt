package dev.jotalac.feature.editor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.ui.components.CustomScaffold
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.feature.editor.ui.components.MarkdownEditor

@Composable
fun EditorScreen(viewModel: EditorViewModel = EditorViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blocks = viewModel.markdownBlocks

    val snackbarHostState: SnackbarHostState =  remember { SnackbarHostState() }


    EditorScreenContent(
        snackbarHostState = snackbarHostState,
        markdownBlocks = blocks,
        filename = state.filename,
        isLoading = state.isLoading,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreenContent(
    snackbarHostState: SnackbarHostState,
    markdownBlocks: List<String>,
    filename: String?,
    isLoading: Boolean,
    onAction: (EditorAction) -> Unit
) {
    CustomScaffold(
        snackbarHostState = snackbarHostState,
        topAppBar = {
            TopAppBar(
                title = { Text(filename ?: "unknown file") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!isLoading) {
                MarkdownEditor(
                    markdownBlocks = markdownBlocks,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LoadingIndicator()
            }
        }
    }
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun EditorScreenPreview() {
    AppTheme {
        EditorScreenContent(
            markdownBlocks = listOf("sdf", "sdf"),
            filename = null,
            onAction = {},
            isLoading = false,
            snackbarHostState = SnackbarHostState()
        )
    }
}