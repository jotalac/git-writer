package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.core.utils.onExternalImageDrop
import dev.jotalac.feature.editor.ui.EditorAction

@Composable
fun MarkdownEditor(
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit,
    modifier: Modifier = Modifier
) {

    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    var cursorTarget by remember { mutableStateOf<TextRange?>(null) }
    var isDraggingImageOver by remember { mutableStateOf(false) }
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
            .onExternalImageDrop(
                onDragOverChange = { isDraggingImageOver = it },
                onImageDropped = { imageBytesList ->
                    val targetIndex = focusedIndex ?: (if (markdownBlocks.isNotEmpty()) markdownBlocks.lastIndex else 0)
                    onAction(
                        EditorAction.PasteImages(
                            imageBytesList = imageBytesList,
                            focusedIndex = targetIndex,
                            onFocusCalculated = { newFocusIndex ->
                                cursorTarget = TextRange(0)
                                focusedIndex = newFocusIndex
                            }
                        )
                    )
                }
            )
            .onPreviewKeyEvent { event ->
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
                if (focusedIndex == index) {
                    onAction(EditorAction.UpdateBlock(index, text))
                }
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
                if (focusedIndex == index) {
                    onAction(EditorAction.AddBlock(index + 1))
                    cursorTarget = TextRange(0)
                    focusedIndex = index + 1
                }
            },
            onSplitBlock = { index, cursorStart ->
                if (focusedIndex == index) {
                    // when enter is presses in the active editing text
                    onAction(EditorAction.SplitBlock(index, cursorStart, { newFocusIndex ->
                        cursorTarget = TextRange(0)
                        focusedIndex = newFocusIndex
                    }))
                }
            },
            onMoveUp = { index ->
                if (focusedIndex == index && index > 0) {
                    cursorTarget = TextRange(markdownBlocks[index - 1].length)
                    focusedIndex = index - 1
                    true
                } else {
                    false
                }
            },
            onMoveDown = { index ->
                if (focusedIndex == index && index < markdownBlocks.size - 1) {
                    cursorTarget = TextRange(markdownBlocks[index + 1].length)
                    focusedIndex = index + 1
                    true
                } else {
                    false
                }
            },
            onBackspaceOnEmpty = { index ->
                if (focusedIndex == index) {
                    onAction(EditorAction.RemoveBlock(index))

                    focusedIndex = if (markdownBlocks.isNotEmpty()) {
                        val newIndex = maxOf(0, index - 1)
                        cursorTarget = TextRange(markdownBlocks[newIndex].length)
                        newIndex
                    } else {
                        null
                    }
                    true
                } else {
                    false
                }
            },
            onBackspaceOnStart = { index ->
                if (focusedIndex != index || index <= 0) true
                else {
                    val originalBlockLength = markdownBlocks[index - 1].length
                    onAction(EditorAction.MergeWithPrevBlock(index))
                    focusedIndex = index - 1
                    cursorTarget = TextRange(originalBlockLength)
                    true
                }
            },
            onAddBlockAtEnd = {
                onAction(EditorAction.AddBlock())
                cursorTarget = TextRange(0)
                focusedIndex = markdownBlocks.lastIndex
            },
            onBlockDelete = { index ->
                onAction(EditorAction.RemoveBlock(index))
                focusedIndex = null
            },
            onImagePasted = { imageBytes ->
                val currentIndex = focusedIndex
                if (currentIndex != null) {
                    onAction(
                        EditorAction.PasteImages(
                            imageBytesList = listOf(imageBytes),
                            focusedIndex = currentIndex,
                            onFocusCalculated = { newFocusIndex ->
                                cursorTarget = TextRange(0)
                                focusedIndex = newFocusIndex
                            }
                        )
                    )
                }
            },

            listState = lazyListState
        )

        AppVerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            listState = lazyListState
        )

        if (!isDesktopPlatform) {
            // show the action bar
        }

        if (isDraggingImageOver) {
            ImageDropOverlay()
        }
    }
}

