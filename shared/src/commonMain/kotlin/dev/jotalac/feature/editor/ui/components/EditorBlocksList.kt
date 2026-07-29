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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.add_new_block_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun MarkdownEditorBlocksList(
    blocks: List<String>,
    focusedIndex: Int?,
    cursorTarget: TextRange?,
    onBlockFocus: (index: Int, cursor: TextRange?) -> Unit,
    onBlockChange: (index: Int, text: String) -> Unit,
    onBlockFocusLost: (index: Int, text: String) -> Unit,
    onEscape: () -> Unit,
    onAddBlockBelow: (index: Int) -> Unit,
    onSplitBlock: (index: Int, cursor: Int) -> Unit,
    onMoveUp: (index: Int) -> Boolean,
    onMoveDown: (index: Int) -> Boolean,
    onBackspaceOnEmpty: (index: Int) -> Boolean,
    onAddBlockAtEnd: () -> Unit,
    onBlockDelete: (index: Int) -> Unit,
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
                    if (focusedIndex == index) {
                        ActiveEditorBlock(
                            initialText = block,
                            cursorTarget = cursorTarget,
                            onTextChange = { newText -> onBlockChange(index, newText) },
                            onFocusLost = { text -> onBlockFocusLost(index, text) },
                            onEscape = onEscape,
                            onAddBlockBelow = { onAddBlockBelow(index) },
                            onSplitBlock = { cursor -> onSplitBlock(index, cursor) },
                            onMoveUp = { onMoveUp(index) },
                            onMoveDown = { onMoveDown(index) },
                            onBackspaceOnEmpty = { onBackspaceOnEmpty(index) }
                        )
                    } else {
                        RenderedEditorBlock(
                            text = block,
                            modifier = Modifier.clickable {
                                onBlockFocus(index, null)
                            },
                            onDeleteClick = { onBlockDelete(index) }
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
                            onAddBlockAtEnd()
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