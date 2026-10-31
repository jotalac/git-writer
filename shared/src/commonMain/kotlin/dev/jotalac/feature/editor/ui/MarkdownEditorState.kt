package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.jotalac.feature.editor.ui.utils.addLink
import dev.jotalac.feature.editor.ui.utils.applyBold
import dev.jotalac.feature.editor.ui.utils.applyInlineCode
import dev.jotalac.feature.editor.ui.utils.applyItalic
import dev.jotalac.feature.editor.ui.utils.continueMarkdownOnNewLine
import dev.jotalac.feature.editor.ui.utils.handleIndentation
import dev.jotalac.feature.editor.ui.utils.insertImageMarkdown
import dev.jotalac.feature.editor.ui.utils.insertTextBetween
import dev.jotalac.feature.editor.ui.utils.moveLines

/**
 * The note being edited, as one text - its cursor, undo history and the edits the editor makes in it.
 * Every change of the text goes to the view model as [EditorAction.UpdateText].
 */
@Stable
class MarkdownEditorState(
    private val noteTextState: State<String>,
    private val onActionState: State<(EditorAction) -> Unit>,
    private val historyManager: EditorHistoryManager,
) {
    var value by mutableStateOf(TextFieldValue(noteTextState.value))
        private set

    /** Whether the text field has the focus - the markdown syntax is shown only then, where the cursor is. */
    var isFocused by mutableStateOf(false)

    // set by the editor's composables
    var requestFieldFocus: (() -> Unit)? = null
    var requestRootFocus: (() -> Unit)? = null

    /** A change from the text field - a new line continues a list, like the Enter key does. */
    fun onValueChange(newValue: TextFieldValue) {
        updateValue(continueMarkdownOnNewLine(value, newValue) ?: newValue)
    }

    /**
     * Shows [newValue] and saves its text. [isSeparateEdit] keeps the edit as one undo step, instead of joining it
     * with the typing around it.
     */
    fun updateValue(newValue: TextFieldValue, isSeparateEdit: Boolean = false) {
        if (newValue.text != value.text) {
            historyManager.record(createCurrentSnapshot(), isSeparateEdit)
            dispatchAction(EditorAction.UpdateText(newValue.text))
        }
        value = newValue
    }

    /** Replaces [start, end) with [replacement] - an edit made by clicking a checkbox or resizing an image. */
    fun replaceRange(start: Int, end: Int, replacement: String) {
        val text = value.text
        if (start < 0 || end > text.length || start > end) return

        val shift = replacement.length - (end - start)
        fun moved(offset: Int) = when {
            offset >= end -> offset + shift
            offset > start -> start + replacement.length
            else -> offset
        }
        updateValue(
            TextFieldValue(
                text = insertTextBetween(text, start, end, replacement),
                selection = TextRange(moved(value.selection.start), moved(value.selection.end)),
            ),
            isSeparateEdit = true,
        )
    }

    private fun dispatchAction(action: EditorAction) {
        onActionState.value(action)
    }

    // the note was changed outside the editor (reloaded after a sync) - keep the cursor where it can be
    fun syncExternalText() {
        val text = noteTextState.value
        if (text == value.text) return

        value = TextFieldValue(
            text = text,
            selection = TextRange(
                value.selection.start.coerceIn(0, text.length),
                value.selection.end.coerceIn(0, text.length),
            ),
        )
    }

    // --- focus and cursor ---

    /** Selects [range] in the text field and focuses it, e.g. a match of the find bar. */
    fun select(range: TextRange) {
        val length = value.text.length
        value = value.copy(selection = TextRange(range.start.coerceIn(0, length), range.end.coerceIn(0, length)))
        requestFieldFocus?.invoke()
    }

    fun focusAt(offset: Int) = select(TextRange(offset))

    fun focusEnd() = focusAt(value.text.length)

    fun clearFocus() {
        requestRootFocus?.invoke()
    }

    // --- undo/redo ---

    private fun createCurrentSnapshot() = EditorHistoryItem(text = value.text, selection = value.selection)

    fun undo() {
        val previousState = historyManager.undo(createCurrentSnapshot()) ?: return
        restore(previousState)
    }

    fun redo() {
        val nextState = historyManager.redo(createCurrentSnapshot()) ?: return
        restore(nextState)
    }

    private fun restore(item: EditorHistoryItem) {
        dispatchAction(EditorAction.UpdateText(item.text))
        value = TextFieldValue(item.text, item.selection)
    }

    // --- edits ---

    fun pasteImages(imageBytesList: List<ByteArray>) {
        if (imageBytesList.isEmpty()) return
        dispatchAction(
            EditorAction.PasteImages(imageBytesList) { imageMarkdown ->
                updateValue(value.insertImageMarkdown(imageMarkdown), isSeparateEdit = true)
            }
        )
    }

    /** A line break that does not continue a list (Shift+Enter). */
    fun insertPlainNewLine() {
        val selection = value.selection
        val newText = insertTextBetween(value.text, selection.min, selection.max, "\n")
        updateValue(TextFieldValue(newText, TextRange(selection.min + 1)))
    }

    fun indent(isUntab: Boolean) = updateValue(handleIndentation(value, isUntab), isSeparateEdit = true)

    fun moveLinesUp() = updateValue(value.moveLines(up = true), isSeparateEdit = true)

    fun moveLinesDown() = updateValue(value.moveLines(up = false), isSeparateEdit = true)

    fun applyBold() = updateValue(value.applyBold(), isSeparateEdit = true)

    fun applyItalic() = updateValue(value.applyItalic(), isSeparateEdit = true)

    fun addLinkTemplate() = updateValue(value.addLink(), isSeparateEdit = true)

    fun applyInlineCode() = updateValue(value.applyInlineCode(), isSeparateEdit = true)
}

@Composable
fun rememberMarkdownEditorState(
    noteText: String,
    onAction: (EditorAction) -> Unit
): MarkdownEditorState {
    val noteTextState = rememberUpdatedState(noteText)
    val onActionState = rememberUpdatedState(onAction)
    val historyManager = remember { EditorHistoryManager() }

    val state = remember { MarkdownEditorState(noteTextState, onActionState, historyManager) }

    LaunchedEffect(noteText) {
        state.syncExternalText()
    }

    return state
}
