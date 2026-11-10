package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp

/*
* What is drawn behind the text - placed with its layout.
* */

// the background of code, across the whole width of its lines
internal fun DrawScope.drawCodeBackgrounds(
    layout: TextLayoutResult,
    backgrounds: List<PlacedDecoration<CodeBackground>>,
    color: Color,
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
