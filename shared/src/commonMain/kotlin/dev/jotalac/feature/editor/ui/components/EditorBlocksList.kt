package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.add_new_block_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun MarkdownEditorBlocksList(
    blocks: List<String>,
    editorState: MarkdownEditorState,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            blocks.forEachIndexed { index, block ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (editorState.focusedIndex == index) {
                        ActiveEditorBlock(
                            editorState = editorState,
                            index = index
                        )
                    } else {
                        RenderedEditorBlock(
                            text = block,
                            modifier = Modifier.clickable {
                                editorState.focusBlock(index, null)
                            },
                            onDeleteClick = { editorState.deleteBlock(index) }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clickable {
                        editorState.addBlockAtEnd()
                    }
            ) {
                Text(
                    text = stringResource(Res.string.add_new_block_message),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(
                modifier = Modifier.height(100.dp)
            )
        }
    }
}