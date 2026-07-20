package dev.jotalac.feature.editor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.feature.editor.ui.components.MarkdownEditor

@Composable
fun EditorScreen(viewModel: EditorViewModel = EditorViewModel()) {
    // launched effects etc - load the markdown text to view
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val blocks = viewModel.markdownBlocks

    if (state.isLoading) {
        LoadingIndicator()
    } else {
        EditorScreenContent(
            markdownBlocks = blocks,
            onAction = viewModel::onAction
        )
    }

}

@Composable
fun EditorScreenContent(
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        MarkdownEditor(
            markdownBlocks = markdownBlocks,
            onAction = onAction,
            modifier = Modifier.fillMaxSize(),
            )
    }
}

@Preview
@Composable
private fun EditorScreenPreview() {
    AppTheme {
        EditorScreenContent(
            markdownBlocks = listOf("sdf", "sdf"),
            onAction = {},
        )
    }
}