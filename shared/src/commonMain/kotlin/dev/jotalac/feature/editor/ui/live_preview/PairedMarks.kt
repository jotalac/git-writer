package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.jotalac.feature.editor.ui.MarkdownEditorState

/*
* The marks typed in pairs, as in Obsidian - `code`, *emphasis*, _emphasis_: typing one at the start of a word
* types its closing one too.
* */

private val PairedMarks = setOf('`', '*', '_')

/**
 * typing a character like *, `, or _ types automatically its pair (when not in the middle of the work)
 */
internal fun TextFieldValue.typePairedMark(char: Char): TextFieldValue? {
    if (char == ' ') return typeSpaceInPair()
    if (char !in PairedMarks) return null

    if (!selection.collapsed) {
        val (start, end) = selection.min to selection.max
        val wrapped = text.substring(0, start) + char + text.substring(start, end) + char + text.substring(end)
        return TextFieldValue(wrapped, TextRange(start + 1, end + 1))
    }

    val cursor = selection.start
    val before = text.getOrNull(cursor - 1)
    val after = text.getOrNull(cursor)
    return when {
        isInEmptyPair(cursor, char) -> insert(cursor, "$char$char", cursor + 1)
        after == char && !isWordStart(before) -> copy(selection = TextRange(cursor + 1))
        // not before a word, which is wrapped by selecting it
        isWordStart(before) && (after == null || !after.isLetterOrDigit() && after != char) ->
            insert(cursor, "$char$char", cursor + 1)
        else -> null
    }
}

/** Backspace inside an empty pair deletes both of its marks - null when it deletes as usual. */
internal fun TextFieldValue.deleteEmptyPair(): TextFieldValue? {
    if (!selection.collapsed) return null
    val cursor = selection.start
    val mark = text.getOrNull(cursor - 1)?.takeIf { it in PairedMarks } ?: return null
    if (!isInEmptyPair(cursor, mark)) return null
    return TextFieldValue(text.removeRange(cursor - 1, cursor + 1), TextRange(cursor - 1))
}

private fun TextFieldValue.typeSpaceInPair(): TextFieldValue? {
    if (!selection.collapsed) return null
    val cursor = selection.start
    val mark = text.getOrNull(cursor - 1)?.takeIf { it == '*' || it == '_' } ?: return null
    // only a single one - "**|**" stays bold
    if (!isInEmptyPair(cursor, mark) || text.getOrNull(cursor - 2) == mark) return null
    return TextFieldValue(text.replaceRange(cursor, cursor + 1, " "), TextRange(cursor + 1))
}

/** Whether the cursor is between as many [mark]s on both sides, with the start of a word before them: `**|**`. */
private fun TextFieldValue.isInEmptyPair(cursor: Int, mark: Char): Boolean {
    val opening = text.substring(0, cursor).takeLastWhile { it == mark }.length
    val closing = text.substring(cursor).takeWhile { it == mark }.length
    return opening > 0 && opening == closing && isWordStart(text.getOrNull(cursor - opening - 1))
}

// the start of the text or of a line, or after whitespace
private fun isWordStart(before: Char?) = before == null || before.isWhitespace()

private fun TextFieldValue.insert(at: Int, inserted: String, cursor: Int) =
    TextFieldValue(text.substring(0, at) + inserted + text.substring(at), TextRange(cursor))

/** The character of a key down typed by the editor itself - the text field must not type it once more. */
internal class TypedHere {
    var char: Int? = null
}

/** Types the key down [event] when it is a paired mark, or a key changing an empty pair - whether it did. */
internal fun typePairedMark(event: KeyEvent, editorState: MarkdownEditorState): Boolean {
    // AltGr is Ctrl+Alt on Windows - it types the marks on many layouts
    if (event.isMetaPressed || event.isCtrlPressed && !event.isAltPressed) return false
    val value = editorState.value
    val newValue = if (event.key == Key.Backspace) value.deleteEmptyPair() else value.typePairedMark(event.utf16CodePoint.toChar())
    editorState.updateValue(newValue ?: return false)
    return true
}
