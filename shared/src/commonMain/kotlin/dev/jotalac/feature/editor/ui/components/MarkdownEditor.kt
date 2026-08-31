package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import dev.jotalac.feature.editor.platform.pickCameraImage
import dev.jotalac.feature.editor.ui.EditorAction
import dev.jotalac.feature.editor.ui.rememberMarkdownEditorState
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

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

    val listScrollState = rememberScrollState()

    val scope = rememberCoroutineScope()

    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    // clear focus when the mobile keyboard is closed
    LaunchedEffect(isKeyboardOpen) {
        if (!isDesktopPlatform && !isKeyboardOpen && editorState.focusedIndex != null) {
            editorState.clearFocus()
            focusManager.clearFocus()
        }
    }


    DisposableEffect(editorState, surfaceFocusRequester) {
        editorState.requestRootFocus = {
            try {
                surfaceFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
        onDispose {
            editorState.requestRootFocus = null
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
                    try {
                        surfaceFocusRequester.requestFocus()
                    } catch (_: Exception) {
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
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                val isShortcutModifier = event.isCtrlPressed || event.isMetaPressed

                // handle undo and redo
                if (isShortcutModifier && event.key == Key.Z) {
                    if (event.isShiftPressed) {
                        editorState.redo()
                    } else {
                        editorState.undo()
                    }
                    return@onPreviewKeyEvent true
                }

                // Handle redo (ctrl+y)
                if (isShortcutModifier && event.key == Key.Y) {
                    editorState.redo()
                    return@onPreviewKeyEvent true
                }


                // Navigation and creation when no block is actively focused
                if (editorState.focusedIndex == null) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            val lastIndex = markdownBlocks.lastIndex
                            if (lastIndex >= 0) {
                                editorState.focusBlock(
                                    lastIndex,
                                    TextRange(markdownBlocks[lastIndex].length)
                                )
                                return@onPreviewKeyEvent true
                            }
                        }

                        Key.DirectionDown -> {
                            if (markdownBlocks.isNotEmpty()) {
                                editorState.focusBlock(
                                    0,
                                    TextRange(markdownBlocks[0].length)
                                )
                                return@onPreviewKeyEvent true
                            }
                        }

                        Key.Enter -> {
                            editorState.addBlockAtEnd()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            },
        contentAlignment = Alignment.TopCenter
    ) {
        MarkdownEditorBlocksList(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isKeyboardOpen) 75.dp else 0.dp) // see what is currently being edited
            ,
            blocks = markdownBlocks,
            editorState = editorState,
            scrollState = listScrollState
        )

        AppVerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            listState = listScrollState
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
                onValueChange = editorState::updateActiveText,
                onImageAdd = {
                    val targetIndex = editorState.focusedIndex
                    scope.launch {
                        val files = FileKit.openFilePicker(type = FileKitType.Image, mode = FileKitMode.Multiple())
                            ?: return@launch
                        val bytesArray = files.map { it.readBytes() }
                        editorState.pasteImages(bytesArray, targetIndex)
                    }
                },
                onCameraOpen = {
                    scope.launch {
                        val bytesArray = pickCameraImage() ?: return@launch
                        editorState.pasteImages(listOf(bytesArray))
                    }
                },
                onUndoRedo = { isUndo ->
                    if (isUndo) editorState.undo() else editorState.redo()
                }
            )
        }
    }
}

