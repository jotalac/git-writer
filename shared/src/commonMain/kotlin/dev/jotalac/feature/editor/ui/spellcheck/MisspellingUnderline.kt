package dev.jotalac.feature.editor.ui.spellcheck

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp

/**
 * Draws a squiggle under every span in [spans], placed with [layout].
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

        // a span can cross line breaks, so underline each line it touches separately
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
