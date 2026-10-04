package dev.jotalac.feature.editor.ui.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp

/**
 * A half-open UTF-16 span in the editor text that the platform spell checker rejected.
 */
data class MisspelledSpan(val start: Int, val end: Int)

/**
 * Misspelled spans of [text], or an empty list when the platform has no spell checker.
 *
 * Desktop resolves them through the spell engine the OS already ships (NSSpellChecker,
 * the Windows Spell Checking API, or Hunspell on Linux). Android and iOS return an empty
 * list on purpose: there the system keyboard already underlines and corrects
 * misspellings, so a second checker would only duplicate it.
 */
@Composable
expect fun rememberMisspelledSpans(text: String): List<MisspelledSpan>

/**
 * Draws a squiggle under every span in [spans], placed with [layout].
 *
 * [layout] has to describe the same text the spans were computed from. That holds for the
 * editor because its header `VisualTransformation` uses `OffsetMapping.Identity`, so
 * original offsets and layout offsets are the same numbers.
 *
 * Spans beyond the layout's text are clamped rather than skipped, so a span list that
 * briefly outlives a text change cannot draw outside the field.
 */
fun DrawScope.drawMisspellingUnderlines(
    layout: TextLayoutResult,
    spans: List<MisspelledSpan>,
    color: Color,
) {
    if (spans.isEmpty()) return

    val textLength = layout.layoutInput.text.length
    val strokeWidth = 1.dp.toPx()
    val amplitude = 1.3.dp.toPx()
    val halfPeriod = 2.5.dp.toPx()

    for (span in spans) {
        var offset = span.start.coerceIn(0, textLength)
        val end = span.end.coerceIn(offset, textLength)

        while (offset < end) {
            val line = layout.getLineForOffset(offset)
            val lineEnd = layout.getLineEnd(line, visibleEnd = true)
            // always advance, so a zero-length or trailing segment cannot loop forever
            val segmentEnd = minOf(end, lineEnd).coerceAtLeast(offset + 1).coerceAtMost(textLength)

            val box = layout.getBoundingBox(offset)
            val edge = layout.getHorizontalPosition(segmentEnd, usePrimaryDirection = true)
            val left = minOf(box.left, edge)
            val right = maxOf(box.left, edge)

            if (right > left) {
                drawSquiggle(
                    left = left,
                    right = right,
                    baseline = box.bottom - strokeWidth,
                    amplitude = amplitude,
                    halfPeriod = halfPeriod,
                    color = color,
                    strokeWidth = strokeWidth,
                )
            }

            offset = segmentEnd
        }
    }
}

private fun DrawScope.drawSquiggle(
    left: Float,
    right: Float,
    baseline: Float,
    amplitude: Float,
    halfPeriod: Float,
    color: Color,
    strokeWidth: Float,
) {
    val path = Path()
    path.moveTo(left, baseline)

    var x = left
    var up = true
    while (x < right) {
        val next = minOf(x + halfPeriod, right)
        val crest = (x + next) / 2f
        val y = if (up) baseline - amplitude else baseline + amplitude
        path.cubicTo(crest, y, crest, y, next, baseline)
        up = !up
        x = next
    }

    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}

/**
 * One row the platform can add to the editor field's own context menu.
 *
 * A disabled row renders dimmed and not clickable, which is how the suggestion block gets a
 * group label: the context menu item model has no separator, so a header is the only way to
 * visually split Cut/Copy/Paste from the spelling entries.
 */
data class SpellcheckAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Suggestions for the misspelled word that contains [anchor] in [text], or `null` when the
 * anchor is missing, the word is spelled correctly, or there is no engine.
 */
expect fun spellcheckMenuFor(text: String, anchor: Int?): SpellcheckMenu?

/** Persists [word] in the user dictionary so the engine stops flagging it. No-op without an engine. */
expect fun addWordToDictionary(word: String)

/**
 * Wraps the editor field so the platform can merge spelling [items] into the field's own
 * context menu. Desktop appends them after Cut/Copy/Paste; mobile renders [content] as is.
 */
@Composable
expect fun SpellcheckMenuHost(
    items: () -> List<SpellcheckAction>,
    content: @Composable () -> Unit,
)

/** A misspelled word and what the menu can do about it. */
data class SpellcheckMenu(
    val word: String,
    val start: Int,
    val end: Int,
    val suggestions: List<String>,
    val addToDictionaryLabel: String,
) {
    /**
     * [text] with [suggestion] in place of the misspelled word.
     *
     * Falls back to replacing the first occurrence of [word] when the engine could not
     * report a range, so a menu built from a stale model still does something sensible.
     */
    fun replacingWordIn(text: String, suggestion: String): String =
        if (start in 0..end && end <= text.length) {
            text.replaceRange(start, end, suggestion)
        } else {
            text.replaceFirst(word, suggestion)
        }
}

/**
 * Reports where a secondary click landed inside the field, in the field's text coordinates.
 *
 * Reads the [PointerEventPass.Initial] pass because the text field consumes the press in
 * the main pass to open its own menu; without the early pass this never fires. The event is
 * deliberately left unconsumed so the field still opens that menu.
 */
fun Modifier.onSpellingMenuRequest(onRequest: (Offset) -> Unit): Modifier = composed {
    val latestRequest = rememberUpdatedState(onRequest)
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.buttons.isSecondaryPressed) {
                    event.changes.firstOrNull()?.let { latestRequest.value(it.position) }
                }
            }
        }
    }
}
