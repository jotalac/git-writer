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

    LaunchedEffect(textFieldValue.selection) {
        bringIntoViewRequester?.bringIntoView()
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
                                textFieldValue = handleNewLineWithinBlock(textFieldValue)
                                onTextChange(textFieldValue.text)
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
                                    textFieldValue = newTextFieldValue
                                    onTextChange(textFieldValue.text)
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

private fun handleMarkdownListContinuation(currentValue: TextFieldValue): TextFieldValue? {
    val text = currentValue.text
    val cursorIndex = currentValue.selection.start
    val textBeforeCursor = text.substring(0, cursorIndex)

    val lastLineIndex = textBeforeCursor.lastIndexOf('\n')
    val currentLineToCursor = textBeforeCursor.substring(lastLineIndex + 1)

    val numberMatch = numberedListRegex.find(currentLineToCursor)


    if (currentLineToCursor == "- " || (numberMatch != null && currentLineToCursor == numberMatch.value)) { // empty list
        val lineStartIndex = lastLineIndex + 1

        val cleanText = insertTextBetween(text, lineStartIndex, cursorIndex)
//        val cleanText = text.substring(0, lineStartIndex) + text.substring(cursorIndex)
        val newCursorPos = lineStartIndex + 1

        return TextFieldValue(text = cleanText, selection = TextRange(newCursorPos))

    } else if (currentLineToCursor.startsWith("- ")) { // dashed list
        val insertText = "\n- "
        val newText = insertTextBetween(text = text, leftSplitIndex = cursorIndex, insertText = insertText)
//        val newText = text.substring(0, cursorIndex) + insertText + text.substring(cursorIndex)

        val newCursor = TextRange(cursorIndex + insertText.length)
        return TextFieldValue(text = newText, selection = newCursor)

    } else if (numberMatch != null) { // numbered list
        // todo - when new item is inserted in the middle change the lines count afterwards
        val currentNumberString = numberMatch.groupValues[1]
        val nextNumber = currentNumberString.toInt() + 1
        val insertText = "\n$nextNumber. "

        val newText = insertTextBetween(text = text, leftSplitIndex = cursorIndex, insertText = insertText)
//        val newText = text.substring(0, cursorIndex) + insertText + text.substring(cursorIndex)
        val newCursorPos = cursorIndex + insertText.length

        return TextFieldValue(text = newText, selection = TextRange(newCursorPos))
    }

    return null

}