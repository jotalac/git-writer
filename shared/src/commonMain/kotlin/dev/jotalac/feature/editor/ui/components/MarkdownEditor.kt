package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks

@Composable
fun MarkdownEditor(
    docTextContent: String,
    modifier: Modifier = Modifier
) {
    val markdownBlocks = remember {
        mutableStateListOf(*chunkMarkdownIntoBlocks(docTextContent).toTypedArray())
    }

    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    var cursorTarget by remember { mutableStateOf<TextRange?>(null) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val surfaceFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusedIndex) {
        if (focusedIndex == null) {
            surfaceFocusRequester.requestFocus()
        }
    }

    fun addNewBlockAtEnd() {
        if (focusedIndex != null) {
            focusManager.clearFocus()
            focusedIndex = null
        } else {
            markdownBlocks.add("")
            cursorTarget = TextRange(0)
            focusedIndex = markdownBlocks.lastIndex
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                    addNewBlockAtEnd()
                    true
                } else false
            }
            .focusRequester(surfaceFocusRequester)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures {
                    addNewBlockAtEnd()
                }
            }
    ) {
        MarkdownEditorBlocksList(
            blocks = markdownBlocks,
            focusedIndex = focusedIndex,
            cursorTarget = cursorTarget,
            onBlockFocus = { index, cursor ->
                cursorTarget = cursor
                focusedIndex = index
            },
            onBlockChange = { index, text ->
                markdownBlocks[index] = text
            },
            onBlockFocusLost = { index, text ->
                if (index < markdownBlocks.size && markdownBlocks[index] == text) {
                    val currentFocused = focusedIndex

                    // if block is empty on focus lost - remove it
                    if (text.isBlank()) {
                        if (index < markdownBlocks.size) {
                            markdownBlocks.removeAt(index)
                            if (currentFocused != null && currentFocused > index) {
                                focusedIndex = currentFocused - 1
                            }
                        }
                    } else {
                        // split the text to blocks of separate types
                        val newChunks = chunkMarkdownIntoBlocks(text)
                        if (newChunks.isEmpty()) {
                            markdownBlocks.removeAt(index)
                            if (currentFocused != null && currentFocused > index) {
                                focusedIndex = currentFocused - 1
                            }
                        } else if (newChunks.size > 1) {
                            println("chunks: $newChunks")
                            markdownBlocks.removeAt(index)
                            markdownBlocks.addAll(index, newChunks)
                            println("Markdown blocks: " + markdownBlocks.joinToString(",\n"))

                            if (currentFocused != null && currentFocused > index) {
                                focusedIndex = currentFocused + (newChunks.size - 1)
                            }
                        }
                    }

                    if (focusedIndex == index) {
                        focusedIndex = null
                    }
                }
            },
            onEscape = {
                focusManager.clearFocus()
                focusedIndex = null
            },
            onAddBlockBelow = { index ->
                markdownBlocks.add(index + 1, "")
                cursorTarget = TextRange(0)
                focusedIndex = index + 1
            },
            onSplitBlock = { index, cursorStart ->
                // when enter is presses in the active editing text
                val text = markdownBlocks[index]
                val textBefore = text.substring(0, cursorStart)
                val textAfter = text.substring(cursorStart)

                // split to text before cursor and after cursor (create valid blocks from the text before and after cursor)
                val chunksBefore = createChunksFromText(textBefore)
                val chunksAfter = createChunksFromText(textAfter)
                
                markdownBlocks.removeAt(index)
                markdownBlocks.addAll(index, chunksBefore + chunksAfter)
                
                cursorTarget = TextRange(0)
                focusedIndex = index + chunksAfter.size
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
                println(index)
                markdownBlocks.forEachIndexed { index, item -> println("$index: $item") }
                markdownBlocks.removeAt(index)
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
                markdownBlocks.add("")
                cursorTarget = TextRange(0)
                focusedIndex = markdownBlocks.lastIndex
            },
            listState = listState
        )
    }
}

private fun createChunksFromText(text: String): List<String> {
    val chunks = chunkMarkdownIntoBlocks(text)
    return if (chunks.isEmpty() && text.isNotEmpty()) {
        listOf(text)
    } else if (text.isEmpty()) {
        listOf("")
    } else {
        chunks
    }
}