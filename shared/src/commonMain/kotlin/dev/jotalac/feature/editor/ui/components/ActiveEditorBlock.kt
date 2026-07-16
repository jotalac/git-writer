package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun ActiveEditorBlock(
    index: Int,
    blockTextContent: String
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocused by remember { mutableStateOf(false) }

    var textFieldValue by remember(index) {
        val initialSelection = cursorTarget ?: TextRange(block.length)
        val safeSelection = TextRange(
            initialSelection.start.coerceIn(0, block.length),
            initialSelection.end.coerceIn(0, block.length)
        )
        mutableStateOf(TextFieldValue(text = block, selection = safeSelection))
    }

    LaunchedEffect(Unit) {
        bringIntoViewRequester.bringIntoView()
    }

    // Auto-scroll when typing pushes the cursor out of view
    LaunchedEffect(textFieldValue.selection) {
        bringIntoViewRequester.bringIntoView()
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            blocks[index] = it.text
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hasFocused = true
                } else if (hasFocused) {
                    // Element lost focus
                    val currentFocused = focusedIndex

                    if (textFieldValue.text.isBlank()) {
                        if (index < blocks.size) {
                            blocks.removeAt(index)
                            if (currentFocused != null && currentFocused > index) {
                                focusedIndex = currentFocused - 1
                            }
                        }
                    } else {
                        val currentText = blocks[index]
                        val newChunks = chunkMarkdownIntoBlocks(currentText)
                        if (newChunks.isEmpty()) {
                            blocks.removeAt(index)
                            if (currentFocused != null && currentFocused > index) {
                                focusedIndex = currentFocused - 1
                            }
                        } else if (newChunks.size > 1) {
                            blocks.removeAt(index)
                            blocks.addAll(index, newChunks)

                            if (currentFocused != null && currentFocused > index) {
                                focusedIndex = currentFocused + (newChunks.size - 1)
                            }
                        }
                    }

                    if (focusedIndex == index) {
                        focusedIndex = null
                    }
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape -> {
                            focusManager.clearFocus()
                            focusedIndex = null
                            true
                        }
                        Key.Enter -> {
                            if (event.isCtrlPressed || event.isShiftPressed) {
                                blocks.add(index + 1, "")
                                cursorTarget = TextRange(0)
                                focusedIndex = index + 1
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionUp -> {
                            val cursorStart = textFieldValue.selection.start
                            val firstNewline = textFieldValue.text.indexOf('\n')
                            val isFirstLine = if (firstNewline == -1) true else cursorStart <= firstNewline

                            if (isFirstLine && index > 0) {
                                cursorTarget = TextRange(blocks[index - 1].length)
                                focusedIndex = index - 1
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionDown -> {
                            val cursorStart = textFieldValue.selection.start
                            val lastNewline = textFieldValue.text.lastIndexOf('\n')
                            val isLastLine = if (lastNewline == -1) true else cursorStart > lastNewline

                            if (isLastLine && index < blocks.size - 1) {
                                cursorTarget = TextRange(0)
                                focusedIndex = index + 1
                                true
                            } else {
                                false
                            }
                        }
                        Key.Backspace -> {
                            // delete the block if the content is empty
                            if (textFieldValue.text.isBlank()) {
                                blocks.removeAt(index)
                                focusedIndex = if (blocks.isNotEmpty() && focusedIndex != null) {
                                    focusedIndex!! - 1
                                } else {
                                    null
                                }
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}