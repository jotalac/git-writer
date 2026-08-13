package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.core.utils.onExternalImageDrop
import dev.jotalac.feature.editor.ui.EditorAction
import dev.jotalac.feature.editor.ui.rememberMarkdownEditorState

@Composable
fun MarkdownEditor(
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorState = rememberMarkdownEditorState(markdownBlocks, onAction)

    var isDraggingImageOver by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val surfaceFocusRequester = remember { FocusRequester() }

    val lazyListState = rememberLazyListState()
    
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(editorState.focusedIndex, markdownBlocks.size) {
        val targetIndex = editorState.focusedIndex
        if (targetIndex == null) {
            surfaceFocusRequester.requestFocus()
        } else if (targetIndex < markdownBlocks.size) {
            // make sure the currently edited items are loaded in the lazy column1
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val isVisible = visibleItems.any { it.index == targetIndex }
            if (!isVisible) {
                lazyListState.scrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .focusRequester(surfaceFocusRequester)
            .focusable()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures {
                    if (editorState.focusedIndex != null) {
                        focusManager.clearFocus()
                        editorState.clearFocus()
                    } else {
                        editorState.addBlockAtEnd()
                    }
                }
            }
            .onExternalImageDrop(
                onDragOverChange = { isDraggingImageOver = it },
                onImageDropped = { imageBytesList ->
                    editorState.pasteImages(imageBytesList)
                }
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && editorState.focusedIndex == null) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            val lastIndex = markdownBlocks.lastIndex
                            if (lastIndex >= 0) {
                                editorState.focusBlock(
                                    lastIndex,
                                    TextRange(markdownBlocks[lastIndex].length)
                                )
                            }
                        }

                        Key.DirectionDown -> {
                            if (markdownBlocks.isNotEmpty()) {
                                editorState.focusBlock(
                                    0,
                                    TextRange(markdownBlocks[0].length)
                                )
                            }
                        }

                        Key.Enter -> {
                            editorState.addBlockAtEnd()
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
            editorState = editorState,
            listState = lazyListState
        )

        AppVerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            listState = lazyListState
        )

        if (isDraggingImageOver) {
            ImageDropOverlay()
        }

        // show the action bar for phones when some block is being edited
        if (!isDesktopPlatform && editorState.focusedIndex != null && isKeyboardOpen) {
            MarkdownKeyboardToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                textFieldValue = editorState.activeTextFieldValue,
                onValueChange = editorState::updateActiveText
            )
        }
    }
}

