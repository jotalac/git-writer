package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.jotalac.core.utils.getImageBytesFromClipboard
import dev.jotalac.core.utils.hasClipboardImage
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import dev.jotalac.feature.editor.ui.utils.handleIndentation
import dev.jotalac.feature.editor.ui.utils.handleMarkdownListContinuation
import dev.jotalac.feature.editor.ui.utils.handleNewLineWithinBlock
import dev.jotalac.feature.editor.ui.utils.isInsideCodeBlock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ActiveEditorBlock(
    editorState: MarkdownEditorState,
    index: Int,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val scope = rememberCoroutineScope()
    var hasFocused by remember { mutableStateOf(false) }

    val textFieldValue = editorState.activeTextFieldValue


    // scroll the viewport to the cursor on typing
    val localBringIntoViewRequester = remember { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(textFieldValue.selection, textLayoutResult) {
        textLayoutResult?.let { layoutResult ->
            try {
                val cursorRect = layoutResult.getCursorRect(textFieldValue.selection.start)
                localBringIntoViewRequester.bringIntoView(
                    cursorRect.copy(
                        top = cursorRect.top - 40f,
                        bottom = cursorRect.bottom + 40f
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    fun updateText(newTextFiledValue: TextFieldValue) {
        editorState.updateActiveText(newTextFiledValue)
    }

    fun handlePlainEnterPress(): Boolean {
        // handle list continuation
        val newTextFieldValue = handleMarkdownListContinuation(textFieldValue)
        return if (newTextFieldValue != null) {
            updateText(newTextFieldValue)

            true
        } else if (isInsideCodeBlock(textFieldValue.text, textFieldValue.selection.start)) {
            // dont break when inside code block
            false
        } else {
            // split block on just enter press
            editorState.splitBlock(textFieldValue.selection.start)
            true
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            editorState.updateActiveText(it)
        },
        onTextLayout = { textLayoutResult = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            handlePlainEnterPress()
        }),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .bringIntoViewRequester(localBringIntoViewRequester)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hasFocused = true
                } else if (hasFocused) {
                    hasFocused = false
                    editorState.evaluateFocusLost(index, textFieldValue.text)
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape -> {
                            editorState.handleEscape()
                            true
                        }

                        Key.Enter -> {
                            if (event.isShiftPressed) {
                                // add new line withing the current block
                                updateText(handleNewLineWithinBlock(textFieldValue))
                                true
                            } else if (event.isCtrlPressed) {
                                // exit the current block and create new
                                editorState.addBlockBelow(index)
                                true
                            } else {
                                handlePlainEnterPress()
                            }
                        }

                        Key.DirectionUp -> {
                            val cursorStart = textFieldValue.selection.start
                            val firstNewline = textFieldValue.text.indexOf('\n')
                            val isFirstLine = if (firstNewline == -1) true else cursorStart <= firstNewline

                            if (isFirstLine) {
                                editorState.moveUp()
                            } else {
                                false
                            }
                        }

                        Key.DirectionDown -> {
                            val cursorStart = textFieldValue.selection.start
                            val lastNewline = textFieldValue.text.lastIndexOf('\n')
                            val isLastLine = if (lastNewline == -1) true else cursorStart > lastNewline

                            if (isLastLine) {
                                editorState.moveDown()
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
                                editorState.backspaceOnEmpty()
                            } else if (textFieldValue.selection.start == 0 && textFieldValue.selection.start == textFieldValue.selection.end) {
                                editorState.backspaceOnStart()
                                true
                            } else {
                                false
                            }
                        }

                        Key.V -> {
                            if (!(event.isCtrlPressed || event.isMetaPressed)) return@onPreviewKeyEvent false
                            if (!hasClipboardImage()) return@onPreviewKeyEvent false

                            scope.launch(Dispatchers.Default) {
                                val imageBytes = getImageBytesFromClipboard()
                                if (imageBytes != null) {
                                    withContext(Dispatchers.Main) {
                                        editorState.pasteImages(listOf(imageBytes))
                                    }
                                }
                            }
                            true
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

