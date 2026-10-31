package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.core.utils.onExternalImageDrop
import dev.jotalac.core.utils.requestFocusSafely
import dev.jotalac.feature.editor.platform.isKeyboardVisible
import dev.jotalac.feature.editor.platform.pickCameraImage
import dev.jotalac.feature.editor.ui.EditorAction
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import dev.jotalac.feature.editor.ui.components.dialogs_overlays.ImageDropOverlay
import dev.jotalac.feature.editor.ui.components.editor_find.FindMatch
import dev.jotalac.feature.editor.ui.components.editor_find.MarkdownFindBar
import dev.jotalac.feature.editor.ui.components.editor_find.MarkdownFindState
import dev.jotalac.feature.editor.ui.live_preview.LivePreviewField
import dev.jotalac.feature.editor.ui.rememberMarkdownEditorState
import dev.jotalac.feature.editor.ui.utils.ignoreLeftoverScroll
import dev.jotalac.feature.editor.ui.utils.restoreScroll
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.empty_note_message
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

val LocalEditorFullWidth = compositionLocalOf { false }

@Composable
fun MarkdownEditor(
    noteText: String,
    onAction: (EditorAction) -> Unit,
    modifier: Modifier = Modifier,
    initialScroll: Int = 0,
    onScrollOffsetChanged: (Int) -> Unit = {},
    findState: MarkdownFindState,
) {
    val editorState = rememberMarkdownEditorState(noteText, onAction)

    var isDraggingImageOver by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val surfaceFocusRequester = remember { FocusRequester() }
    val findQueryFocusRequester = remember { FocusRequester() }

    val scrollState = rememberScrollState()
    val fullWidth = LocalEditorFullWidth.current

    // restore where this note was scrolled to, then report the position so it can be restored next time -
    // only after restoring, the positions on the way there are not where the user was
    LaunchedEffect(scrollState) {
        scrollState.restoreScroll(initialScroll)
        snapshotFlow { scrollState.value }
            .collect { onScrollOffsetChanged(it) }
    }

    val scope = rememberCoroutineScope()

    val isKeyboardOpen = isKeyboardVisible()

    DisposableEffect(editorState, surfaceFocusRequester) {
        editorState.requestRootFocus = {
            focusManager.clearFocus()
            surfaceFocusRequester.requestFocusSafely()
        }
        onDispose {
            editorState.requestRootFocus = null
        }
    }

    LaunchedEffect(Unit) {
        if (isDesktopPlatform) surfaceFocusRequester.requestFocusSafely()
    }

    fun selectMatch(match: FindMatch?) {
        match?.let { editorState.select(TextRange(it.start, it.end)) }
    }

    fun closeFindBar() {
        findState.close()
        surfaceFocusRequester.requestFocusSafely()
    }

    Box(
        modifier = modifier
            .ignoreLeftoverScroll()
            .focusRequester(surfaceFocusRequester)
            .focusable()
            .imePadding()
            .onExternalImageDrop(
                onDragOverChange = { isDraggingImageOver = it },
                onImageDropped = { imageBytesList ->
                    editorState.pasteImages(imageBytesList)
                }
            )
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && handleEditorShortcut(
                    event = event,
                    editorState = editorState,
                    findState = findState,
                    onAction = onAction,
                    onFindQueryFocus = { findQueryFocusRequester.requestFocusSafely() },
                    onSelectMatch = ::selectMatch,
                    onCloseFindBar = ::closeFindBar,
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    bottom = if (isKeyboardOpen) 75.dp else 0.dp,
                    top = if (findState.isOpen) 50.dp else 0.dp
                ), // see what is currently being edited
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (fullWidth) Dp.Unspecified else 800.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // text when the editor is empty
                if (editorState.value.text.trim().isEmpty() && !editorState.isFocused) {
                    Text(text = stringResource(Res.string.empty_note_message), color = MaterialTheme.colorScheme.outline)
                }

                LivePreviewField(editorState = editorState)


                // the space under the note - a click there writes at its end
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { editorState.focusEnd() }
                        }
                )
            }
        }

        if (findState.isOpen) {
            MarkdownFindBar(
                findState = findState,
                focusRequester = findQueryFocusRequester,
                onQueryChange = { findState.updateQuery(it, editorState.value.text) },
                onNext = { selectMatch(findState.findNext()) },
                onPrevious = { selectMatch(findState.findPrevious()) },
                onClose = ::closeFindBar,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .widthIn(max = 500.dp)
                    .padding(16.dp),
            )
        }

        AppVerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            listState = scrollState
        )

        if (isDraggingImageOver) {
            ImageDropOverlay()
        }

        // show the action bar for phones while the note is being edited
        if (!isDesktopPlatform && editorState.isFocused && isKeyboardOpen) {
            MarkdownKeyboardToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                textFieldValue = editorState.value,
                onValueChange = { editorState.updateValue(it, isSeparateEdit = true) },
                onImageAdd = {
                    scope.launch {
                        val files = FileKit.openFilePicker(type = FileKitType.Image, mode = FileKitMode.Multiple())
                            ?: return@launch
                        val bytesArray = files.map { it.readBytes() }
                        editorState.pasteImages(bytesArray)
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

/** The shortcuts of the whole editor (a key down [event]) - undo, the tabs, the find bar. */
private fun handleEditorShortcut(
    event: KeyEvent,
    editorState: MarkdownEditorState,
    findState: MarkdownFindState,
    onAction: (EditorAction) -> Unit,
    onFindQueryFocus: () -> Unit,
    onSelectMatch: (FindMatch?) -> Unit,
    onCloseFindBar: () -> Unit,
): Boolean {
    val isShortcut = event.isCtrlPressed || event.isMetaPressed

    if (isShortcut) {
        val action: (() -> Unit)? = when (event.key) {
            Key.Z -> if (event.isShiftPressed) editorState::redo else editorState::undo
            Key.Y -> editorState::redo
            Key.W -> { -> onAction(EditorAction.CloseActiveTab) }
            Key.T -> { -> onAction(EditorAction.NewTab) }
            Key.Tab -> { -> onAction(if (event.isShiftPressed) EditorAction.PreviousTab else EditorAction.NextTab) }
            else -> null
        }
        if (action != null) {
            action()
            return true
        }
    }

    // open the find bar (Ctrl+F, or / outside the text), or focus its query field when it is open already
    if (isShortcut && event.key == Key.F || event.key == Key.Slash && !editorState.isFocused) {
        if (findState.isOpen) onFindQueryFocus() else findState.open()
        return true
    }

    // moving between the matches while the find bar is open
    if (findState.isOpen) {
        when (event.key) {
            Key.Enter -> onSelectMatch(if (event.isShiftPressed) findState.findPrevious() else findState.findNext())
            Key.Escape -> onCloseFindBar()
            else -> return false
        }
        return true
    }

    return false
}
