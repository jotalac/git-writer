package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.feature.editor.ui.EditorAction

@Composable
fun MarkdownEditor(
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit,
    modifier: Modifier = Modifier
) {

    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    var cursorTarget by remember { mutableStateOf<TextRange?>(null) }
    val focusManager = LocalFocusManager.current

    val surfaceFocusRequester = remember { FocusRequester() }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(focusedIndex) {
        if (focusedIndex == null) {
            surfaceFocusRequester.requestFocus()
        } else {
            // make sure the currently edited items are loaded in the lazy column
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val isVisible = visibleItems.any { it.index == focusedIndex }
            if (!isVisible) {
                lazyListState.scrollToItem(focusedIndex!!)
            }
        }
    }

    fun addNewBlockAtEnd() {
        if (focusedIndex != null) {
            focusManager.clearFocus()
            focusedIndex = null
        } else {
            onAction(EditorAction.AddBlock())
            cursorTarget = TextRange(0)
            focusedIndex = markdownBlocks.lastIndex
        }
    }

    Box(
        modifier = modifier
            .focusRequester(surfaceFocusRequester)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures {
                    addNewBlockAtEnd()
                }
            }
            .onPreviewKeyEvent{ event ->
                if (event.type == KeyEventType.KeyDown && focusedIndex == null) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            focusedIndex = markdownBlocks.lastIndex
                            cursorTarget = TextRange(markdownBlocks[focusedIndex!!].length)
                        }
                        Key.DirectionDown -> {
                            focusedIndex = 0
                            cursorTarget = TextRange(markdownBlocks[focusedIndex!!].length)
                        }
                        Key.Enter -> {
                            addNewBlockAtEnd()
                        }
                    }
                }
                false
            },
        contentAlignment = Alignment.TopCenter
    ) {
        MarkdownEditorBlocksList(
            modifier = Modifier.widthIn(max = 800.dp),
            blocks = markdownBlocks,
            focusedIndex = focusedIndex,
            cursorTarget = cursorTarget,
            onBlockFocus = { index, cursor ->
                cursorTarget = cursor
                focusedIndex = index
            },
            onBlockChange = { index, text ->
                onAction(EditorAction.UpdateBlock(index, text))
            },
            onBlockFocusLost = { index, text ->
                onAction(
                    EditorAction.EvaluateBlockOnFocusLost(
                    index = index,
                    currentFocusedIndex = focusedIndex,
                    onFocusAdjusted = {
                        focusedIndex = index
                    }
                ))
            },
            onEscape = {
                focusManager.clearFocus()
                focusedIndex = null
            },
            onAddBlockBelow = { index ->
                onAction(EditorAction.AddBlock(index + 1))
                cursorTarget = TextRange(0)
                focusedIndex = index + 1
            },
            onSplitBlock = { index, cursorStart ->
                // when enter is presses in the active editing text
                onAction(EditorAction.SplitBlock(index, cursorStart, { newFocusIndex ->
                    cursorTarget = TextRange(0)
                    focusedIndex = newFocusIndex
                }))
            },
            onMoveUp = { index ->
                if (index > 0) {
                    cursorTarget = TextRange(markdownBlocks[index - 1].length)
                    focusedIndex = index - 1
                    true
                } else {
                    false
                }
            },
            onMoveDown = { index ->
                if (index < markdownBlocks.size - 1) {
                    cursorTarget = TextRange(markdownBlocks[index + 1].length)
                    focusedIndex = index + 1
                    true
                } else {
                    false
                }
            },
            onBackspaceOnEmpty = { index ->
                onAction(EditorAction.RemoveBlock(index))
                focusedIndex = if (markdownBlocks.isNotEmpty()) {
                    val newIndex = maxOf(0, index - 1)
                    cursorTarget = TextRange(markdownBlocks[newIndex].length)
                    newIndex
                } else {
                    null
                }
                true
            },
            onAddBlockAtEnd = {
                onAction(EditorAction.AddBlock())
                cursorTarget = TextRange(0)
                focusedIndex = markdownBlocks.lastIndex
            },
            listState = lazyListState
        )

        AppVerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            listState = lazyListState
        )
    }
}

