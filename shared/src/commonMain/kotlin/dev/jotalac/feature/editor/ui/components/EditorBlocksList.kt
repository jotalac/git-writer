package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
    listState: LazyListState = rememberLazyListState()
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(blocks) { index, block ->
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

            item {
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
            }
        }

    }
}