package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp

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
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        itemsIndexed(blocks) { index, block ->
            val bringIntoViewRequester = remember { BringIntoViewRequester() }


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .bringIntoViewRequester(bringIntoViewRequester)
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
                        onBackspaceOnEmpty = { onBackspaceOnEmpty(index) },
                        bringIntoViewRequester = bringIntoViewRequester
                    )
                } else {
                    RenderedEditorBlock(
                        text = block,
                        modifier = Modifier.clickable {
                            onBlockFocus(index, null)
                        }
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
                    text = "Tap to add new text...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}