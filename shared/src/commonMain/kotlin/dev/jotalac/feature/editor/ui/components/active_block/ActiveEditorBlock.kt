package dev.jotalac.feature.editor.ui.components.active_block

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import dev.jotalac.core.utils.getImageBytesFromClipboard
import dev.jotalac.core.utils.hasClipboardImage
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import dev.jotalac.feature.editor.ui.utils.*
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

    fun updateText(newTextFiledValue: TextFieldValue, fromIndex: Int? = null) {
        editorState.updateActiveText(newTextFiledValue, fromIndex)
    }

    fun handlePlainEnterPress() {
        // handle list continuation
        val currentValue = editorState.activeTextFieldValue
        val newTextFieldValue = handleMarkdownListContinuation(currentValue)
        if (newTextFieldValue != null) {
            updateText(newTextFieldValue)

        } else if (isInsideCodeBlock(currentValue.text, currentValue.selection.start)) {
            // dont break code when inside code block
            val text = currentValue.text
            val cursor = currentValue.selection.start.coerceIn(0, text.length)
            val newText = text.substring(0, cursor) + "\n" + text.substring(cursor)

            updateText(TextFieldValue(newText, TextRange(cursor + 1)))

        } else {
            // split block on just enter press
            editorState.splitBlock(currentValue.selection.start.coerceIn(0, currentValue.text.length))
        }


    }


    // make the header style WYSIWYG
    val firstLine = textFieldValue.text.substringBefore('\n')
    val headerLevel = getHeaderLevel(firstLine)
    val activeTextStyle = getHeaderFontSize(headerLevel).copy(color = MaterialTheme.colorScheme.onSurface)
    val prefixDimStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
    val prefixVisualTransformation = remember(prefixDimStyle, headerLevel) {
        if (headerLevel == 0) VisualTransformation.None
        else VisualTransformation { text ->
            val prefixLen = minOf(headerLevel + 1, text.text.length)
            val builder = AnnotatedString.Builder(text)
            builder.addStyle(prefixDimStyle, 0, prefixLen)
            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            editorState.updateActiveText(it, index)
        },
        onTextLayout = { textLayoutResult = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            handlePlainEnterPress()
        }),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 9.dp)
            .bringIntoViewRequester(localBringIntoViewRequester)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hasFocused = true
                } else if (hasFocused) {
                    hasFocused = false
                    editorState.evaluateFocusLost(index)
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
                            } else if (event.isCtrlPressed) {
                                // exit the current block and create new
                                editorState.addBlockBelow(index)
                            } else {
                                handlePlainEnterPress()
                            }
                            true
                        }

                        Key.DirectionUp -> {
                            val cursorOffset = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
                            val isFirstLine = textLayoutResult?.let { layout ->
                                val offset = cursorOffset.coerceIn(0, layout.layoutInput.text.length)
                                layout.getLineForOffset(offset) == 0
                            } ?: run {
                                val firstNewline = textFieldValue.text.indexOf('\n')
                                if (firstNewline == -1) true else cursorOffset <= firstNewline
                            }

                            if (event.isAltPressed) {
                                editorState.swapBlockUp()
                                true
                            } else if (isFirstLine) {
                                editorState.moveUp()
                            } else {
                                false
                            }
                        }

                        Key.DirectionDown -> {
                            val cursorOffset = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
                            val isLastLine = textLayoutResult?.let { layout ->
                                val offset = cursorOffset.coerceIn(0, layout.layoutInput.text.length)
                                layout.getLineForOffset(offset) == layout.lineCount - 1
                            } ?: run {
                                val lastNewline = textFieldValue.text.lastIndexOf('\n')
                                if (lastNewline == -1) true else cursorOffset > lastNewline
                            }

                            if (event.isAltPressed) {
                                editorState.swapBlockDown()
                                true
                            } else if (isLastLine) {
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

                        Key.B -> {
                            if (event.isCtrlPressed || event.isMetaPressed) {
                                editorState.applyBold()
                                true
                            } else false
                        }

                        Key.I -> {
                            if (event.isCtrlPressed || event.isMetaPressed) {
                                editorState.applyItalic()
                                true
                            } else false
                        }

                        Key.K -> {
                            if (event.isCtrlPressed || event.isMetaPressed) {
                                editorState.addLinkTemplate()
                                true
                            } else false
                        }

                        Key.E -> {
                            if (event.isCtrlPressed || event.isMetaPressed) {
                                editorState.applyInlineCode()
                                true
                            } else false
                        }


                        else -> false
                    }
                } else {
                    false
                }
            },
        textStyle = activeTextStyle,
        visualTransformation = prefixVisualTransformation,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

