package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp

/*
* What is drawn behind the text - placed with its layout.
* */

// the background of code, across the whole width of its lines - with the language of a code block in its corner,
// on the line of the hidden fence
internal fun DrawScope.drawCodeBackgrounds(
    layout: TextLayoutResult,
    backgrounds: List<PlacedDecoration<CodeBackground>>,
    color: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val radius = CornerRadius(6.dp.toPx())
    for (background in backgrounds) {
        val top = layout.getLineTop(layout.getLineForOffset(background.start))
        val bottom = layout.getLineBottom(layout.getLineForOffset(background.end.coerceAtLeast(background.start)))
        drawRoundRect(
            color = color,
            topLeft = Offset(-4.dp.toPx(), top),
            size = Size(size.width + 8.dp.toPx(), bottom - top),
            cornerRadius = radius,
        )

        // display the codeblock language
        val label = background.decoration.label ?: continue
        val measured = textMeasurer.measure(label.replaceFirstChar { it.uppercase() }, labelStyle, maxLines = 1)
        val firstLine = layout.getLineForOffset(background.start)
        val lineHeight = layout.getLineBottom(firstLine) - layout.getLineTop(firstLine)
        drawText(
            measured,
            topLeft = Offset(size.width - measured.size.width - 8.dp.toPx(), top + (lineHeight - measured.size.height) / 2),
        )
    }
}

// the bullets of the list items - a dot, a ring and a square, taking turns with every nesting level
internal fun DrawScope.drawListBullets(
    layout: TextLayoutResult,
    bullets: List<PlacedDecoration<ListBullet>>,
    color: Color,
    fontSize: Float,
) {
    val radius = fontSize * 0.2f
    for (bullet in bullets) {
        val box = layout.getBoundingBox(bullet.start)
        // the middle of the small letters of the line
        val center = Offset(box.center.x, layout.getLineBaseline(layout.getLineForOffset(bullet.start)) - fontSize * 0.28f)
        when (bullet.decoration.depth % 3) {
            0 -> drawCircle(color, radius, center)
            1 -> drawCircle(color, radius - 0.5.dp.toPx(), center, style = Stroke(width = 1.dp.toPx()))
            else -> drawRect(color, topLeft = center - Offset(radius, radius) * 0.9f, size = Size(radius, radius) * 1.8f)
        }
    }
}

// a bar beside each quote, where its `>` was - in the color of its alert type for a GitHub alert
internal fun DrawScope.drawQuoteBars(
    layout: TextLayoutResult,
    bars: List<PlacedDecoration<QuoteBar>>,
    color: Color,
) {
    val width = 3.dp.toPx()
    for (bar in bars) {
        val top = layout.getLineTop(layout.getLineForOffset(bar.start))
        val bottom = layout.getLineBottom(layout.getLineForOffset((bar.end - 1).coerceAtLeast(bar.start)))
        val x = layout.getHorizontalPosition(bar.anchor, usePrimaryDirection = true) + 2.dp.toPx()
        drawRoundRect(
            color = bar.decoration.alert?.color ?: color,
            topLeft = Offset(x, top),
            size = Size(width, bottom - top),
            cornerRadius = CornerRadius(width / 2),
        )
    }
}
