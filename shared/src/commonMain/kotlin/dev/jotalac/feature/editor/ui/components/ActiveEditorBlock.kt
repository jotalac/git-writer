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
import androidx.compose.foundation.relocation.BringIntoViewRequester
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
    initialText: String,
    cursorTarget: TextRange?,
    onTextChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    onEscape: () -> Unit,
    onAddBlockBelow: () -> Unit,
    onSplitBlock: (Int) -> Unit,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    onBackspaceOnEmpty: () -> Boolean,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    bringIntoViewRequester: BringIntoViewRequester? = null
) {
    var hasFocused by remember { mutableStateOf(false) }

    var textFieldValue by remember {
        val initialSelection = cursorTarget ?: TextRange(initialText.length)
        val safeSelection = TextRange(
            initialSelection.start.coerceIn(0, initialText.length),
            initialSelection.end.coerceIn(0, initialText.length)
        )
        mutableStateOf(TextFieldValue(text = initialText, selection = safeSelection))
    }

    LaunchedEffect(initialText, cursorTarget) {
        if (initialText != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = initialText,
                selection = cursorTarget ?: TextRange(
                    textFieldValue.selection.start.coerceIn(0, initialText.length),
                    textFieldValue.selection.end.coerceIn(0, initialText.length)
                )
            )
        }
    }

    LaunchedEffect(textFieldValue.selection) {
        bringIntoViewRequester?.bringIntoView()
    }

    fun updateText(newTextFiledValue: TextFieldValue) {
        textFieldValue = newTextFiledValue
        onTextChange(newTextFiledValue.text)
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onTextChange(it.text)
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hasFocused = true
                } else if (hasFocused) {
                    hasFocused = false
                    onFocusLost(textFieldValue.text)
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape -> {
                            onEscape()
                            true
                        }
                        Key.Enter -> {
                            if (event.isShiftPressed) {
                                // add new line withing the current block
                                updateText(handleNewLineWithinBlock(textFieldValue))
                                true
                            }
                            else if (event.isCtrlPressed) {
                                // exit the current block and create new
                                onAddBlockBelow()
                                true
                            } else {
                                // handle list continuation
                                val newTextFieldValue = handleMarkdownListContinuation(textFieldValue)
                                if (newTextFieldValue != null) {
                                    updateText(newTextFieldValue)

                                    true
                                } else if (isInsideCodeBlock(textFieldValue.text, textFieldValue.selection.start)) {
                                    // dont break when inside code block
                                    false
                                } else {
                                    // split block on just enter press
                                    onSplitBlock(textFieldValue.selection.start)
                                    true
                                }
                            }
                        }
                        Key.DirectionUp -> {
                            val cursorStart = textFieldValue.selection.start
                            val firstNewline = textFieldValue.text.indexOf('\n')
                            val isFirstLine = if (firstNewline == -1) true else cursorStart <= firstNewline

                            if (isFirstLine) {
                                onMoveUp()
                            } else {
                                false
                            }
                        }
                        Key.DirectionDown -> {
                            val cursorStart = textFieldValue.selection.start
                            val lastNewline = textFieldValue.text.lastIndexOf('\n')
                            val isLastLine = if (lastNewline == -1) true else cursorStart > lastNewline

                            if (isLastLine) {
                                onMoveDown()
                            } else {
                                false
                            }
                        }
                        Key.Tab -> {
                            updateText(handleIndentation(textFieldValue, event.isShiftPressed))

                            true
                        }
                        Key.Backspace -> {
                            if (textFieldValue.text.isBlank()) {
                                onBackspaceOnEmpty()
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

private fun isInsideCodeBlock(text: String, cursorIndex: Int): Boolean {
    val textBeforeCursor = text.substring(0, cursorIndex)
    val fenceCount = textBeforeCursor.split("```").size - 1
    return fenceCount % 2 != 0
}

private val numberedListRegex = Regex("^(\\d+)\\. ")

private fun insertTextBetween(text: String, leftSplitIndex: Int, rightSplitIndex: Int = leftSplitIndex, insertText: String = ""): String {
    return text.substring(0, leftSplitIndex) + insertText + text.substring(rightSplitIndex)
}

private fun handleNewLineWithinBlock(currentValue: TextFieldValue): TextFieldValue {
    val text = currentValue.text
    val cursorIndex = currentValue.selection.start
    val newText = insertTextBetween(text = text, leftSplitIndex = cursorIndex, insertText = "  \n")

    return TextFieldValue(text=newText, selection = TextRange(cursorIndex + 3))
}

private fun exitListContinuation(lastLineIndex: Int, cursorIndex: Int, text: String): TextFieldValue {
    val lineStartIndex = lastLineIndex + 1

    val cleanText = insertTextBetween(text, lineStartIndex, cursorIndex)
    val newCursorPos = lineStartIndex + 1

    return TextFieldValue(text = cleanText, selection = TextRange(newCursorPos))
}

private fun handleUnorderedList(fullText: String, currentLineText: String, lastLineIndex: Int, cursorIndex: Int, startSpaces: Int): TextFieldValue? {
    if (currentLineText == "- ") {
        return exitListContinuation(lastLineIndex, cursorIndex, fullText)
    } else if (currentLineText.startsWith("- ")) {
        val insertText = "\n" + " ".repeat(startSpaces) + "- "
        val newText = insertTextBetween(text = fullText, leftSplitIndex = cursorIndex, insertText = insertText)

        val newCursor = TextRange(cursorIndex + insertText.length)
        return TextFieldValue(text = newText, selection = newCursor)
    }

    return null
}

private fun handleOrderedList(fullText: String, currentLineText: String, lastLineIndex: Int, cursorIndex: Int, spacesStart: Int): TextFieldValue? {
    val numberMatch = numberedListRegex.find(currentLineText)

    if (numberMatch != null && currentLineText == numberMatch.value) {
        return exitListContinuation(lastLineIndex, cursorIndex, fullText)

    } else if (numberMatch != null) {
        // todo - when new item is inserted in the middle change the lines count afterwards
        val currentNumberString = numberMatch.groupValues[1]
        val nextNumber = currentNumberString.toInt() + 1
        val insertText = "\n" + " ".repeat(spacesStart) + "$nextNumber. "

        val newText = insertTextBetween(text = fullText, leftSplitIndex = cursorIndex, insertText = insertText)
        val newCursorPos = cursorIndex + insertText.length

        return TextFieldValue(text = newText, selection = TextRange(newCursorPos))
    }

    return null
}

private fun handleBlockQuotes(fullText: String, currentLineText: String, lastLineIndex: Int, cursorIndex: Int): TextFieldValue? {
    if (currentLineText.trim() == ">") {
        return exitListContinuation(lastLineIndex, cursorIndex, fullText)
    } else if (currentLineText.startsWith(">")) {
        val insertText = "  \n> "
        val newText = insertTextBetween(text = fullText, leftSplitIndex = cursorIndex, insertText = insertText)

        val newCursor = TextRange(cursorIndex + insertText.length)
        return TextFieldValue(text = newText, selection = newCursor)
    }

    return null
}

private fun handleMarkdownListContinuation(currentValue: TextFieldValue): TextFieldValue? {
    val text = currentValue.text
    val cursorIndex = currentValue.selection.start
    val textBeforeCursor = text.substring(0, cursorIndex)

    val lastLineIndex = textBeforeCursor.lastIndexOf('\n')
    val currentLineToCursor = textBeforeCursor.substring(lastLineIndex + 1)

    val spacesStart = currentLineToCursor.takeWhile { it == ' ' }.length
    val cleanCurrentLine = currentLineToCursor.trimStart()

    return handleUnorderedList(text, cleanCurrentLine, lastLineIndex, cursorIndex, spacesStart)
        ?: handleOrderedList(text, cleanCurrentLine, lastLineIndex, cursorIndex, spacesStart)
        ?: handleBlockQuotes(text, cleanCurrentLine, lastLineIndex, cursorIndex)
}


private fun handleIndentation(currentValue: TextFieldValue, isUntab: Boolean): TextFieldValue {
    val text = currentValue.text
    val rawStart = currentValue.selection.start
    val rawEnd = currentValue.selection.end

    val minCursor = minOf(rawStart, rawEnd)
    val maxCursor = maxOf(rawStart, rawEnd)

    val startLineIndex = text.lastIndexOf('\n', minCursor - 1).coerceAtLeast(-1) + 1
    val endLineIndex = text.indexOf('\n', maxCursor).let { if (it == -1) text.length else it }

    val textBefore = text.substring(0, startLineIndex)
    val targetLines = text.substring(startLineIndex, endLineIndex)
    val textAfter = text.substring(endLineIndex)

    var firstLineShift = 0
    var totalShift = 0

    val modifiedLines = targetLines.split('\n').mapIndexed { index, line ->
        val shift: Int
        val newLine = if (isUntab) {
            // remove up to 4 leading spaces
            val leadingSpaces = line.takeWhile { it == ' ' }.length
            when {
                leadingSpaces > 0 -> {
                    val spacesToRemove = minOf(leadingSpaces, 4)
                    shift = -spacesToRemove
                    line.substring(spacesToRemove)
                }
                // fallback for tab signs
                line.startsWith("\t") -> {
                    shift = -1
                    line.substring(1)
                }
                // no indentation found
                else -> {
                    shift = 0
                    line
                }
            }
        } else {
            // add tab
            shift = 4
            "    $line"
        }

        if (index == 0) firstLineShift = shift
        totalShift += shift

        newLine
    }.joinToString("\n")

    // skip if nothing changes
    if (totalShift == 0) return currentValue

    // reconstruct the whole text
    val newText = textBefore + modifiedLines + textAfter

    val newMin = maxOf(startLineIndex, minCursor + firstLineShift).coerceIn(0, newText.length)
    val newMax = maxOf(newMin, maxCursor + totalShift).coerceIn(0, newText.length)


    val isReversed = rawStart > rawEnd
    val newSelection = if (isReversed) TextRange(newMax, newMin) else TextRange(newMin, newMax)

    return TextFieldValue(text = newText, selection = newSelection)
}