package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping

/**
 * How the cursor is moved in the editor
 * Eg moving whoel lines up and down,
 */
internal class LineCursorMove {
    // the x the cursor keeps over moves up and down, and the selection the last such move made
    private var x: Float? = null
    private var movedTo: TextRange? = null

    /** The selection after [event], or null when it is not a key moving by lines. */
    fun selectionFor(
        event: KeyEvent,
        selection: TextRange,
        layout: TextLayoutResult,
        mapping: OffsetMapping,
        textLength: Int,
    ): TextRange? {
        if (event.isAltPressed || event.isCtrlPressed) return null
        val shown = mapping.originalToTransformed(selection.end).coerceIn(0, layout.layoutInput.text.length)
        val line = layout.getLineForOffset(shown)
        fun lineStart() = mapping.transformedToOriginal(layout.getLineStart(line))
        fun lineEnd() = mapping.transformedToOriginal(layout.lineEndCaret(line))

        val end = when {
            // Cmd+arrow on macOS goes to the start or the end of the note, or of the line
            event.isMetaPressed -> when (event.key) {
                Key.DirectionUp -> 0
                Key.DirectionDown -> textLength
                Key.DirectionLeft -> lineStart()
                Key.DirectionRight -> lineEnd()
                else -> return null
            }
            else -> when (event.key) {
                Key.DirectionUp -> verticalTarget(line - 1, shown, selection, layout, mapping, textLength)
                Key.DirectionDown -> verticalTarget(line + 1, shown, selection, layout, mapping, textLength)
                Key.MoveHome -> lineStart()
                Key.MoveEnd -> lineEnd()
                else -> return null
            }
        }

        val isVertical = event.key == Key.DirectionUp || event.key == Key.DirectionDown
        if (!isVertical || event.isMetaPressed) x = null
        return (if (event.isShiftPressed) TextRange(selection.start, end) else TextRange(end)).also { movedTo = it }
    }

    private fun verticalTarget(
        targetLine: Int,
        shown: Int,
        selection: TextRange,
        layout: TextLayoutResult,
        mapping: OffsetMapping,
        textLength: Int,
    ): Int {
        // keeps the x of the first move, so the cursor comes back to its column after a shorter line
        val x = this.x.takeIf { movedTo == selection } ?: layout.getCursorRect(shown).left
        this.x = x
        return when {
            targetLine < 0 -> 0
            targetLine >= layout.lineCount -> textLength
            else -> {
                val y = (layout.getLineTop(targetLine) + layout.getLineBottom(targetLine)) / 2
                val offset = layout.getOffsetForPosition(Offset(x, y))
                mapping.transformedToOriginal(offset.coerceIn(layout.getLineStart(targetLine), layout.lineEndCaret(targetLine)))
            }
        }
    }
}

// get last position on the line for the cursor -> it respects the unprintable characters
private fun TextLayoutResult.lineEndCaret(line: Int): Int {
    val end = getLineEnd(line)
    return if (end > getLineStart(line) && getLineForOffset(end) != line) end - 1 else end
}
