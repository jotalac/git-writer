package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.Latex
import dev.jotalac.feature.editor.ui.components.markdown_rendering.RenderedMarkdownBlock
import kotlin.math.ceil
import kotlin.math.roundToInt

/*
* The composables drawn over the text field: the widgets (images, tables, maths) on the lines the text keeps for
* them, the formulas inside text and the boxes of the tasks. They are placed with the text's layout in the same
* layout pass as the text field - placed with the layout of the previous frame, they would jump around while typing.
* */

/** A composable drawn over the text. */
internal sealed interface OverlayItem {
    val key: Any

    /** Measured for a text [width] wide, and where it goes - null without a layout. */
    fun measure(measurable: Measurable, layout: TextLayoutResult?, width: Int): Pair<Placeable, IntOffset?>
}

// on the line kept for it
private data class WidgetItem(override val key: Any, val placed: PlacedDecoration<Widget>, val source: String) : OverlayItem {
    override fun measure(measurable: Measurable, layout: TextLayoutResult?, width: Int): Pair<Placeable, IntOffset?> =
        measurable.measure(Constraints(minWidth = width, maxWidth = width)) to
            layout?.let { IntOffset(0, it.getLineTop(it.getLineForOffset(placed.start)).toInt()) }
}

// over the space kept for it
private data class CheckboxItem(override val key: Any, val placed: PlacedDecoration<TaskCheckbox>) : OverlayItem {
    override fun measure(measurable: Measurable, layout: TextLayoutResult?, width: Int): Pair<Placeable, IntOffset?> {
        val box = layout?.getBoundingBox(placed.start)
            ?: return measurable.measure(Constraints.fixed(0, 0)) to null
        val size = Constraints.fixed(box.width.toInt().coerceAtLeast(0), box.height.toInt().coerceAtLeast(0))
        return measurable.measure(size) to IntOffset(box.left.toInt(), box.top.toInt())
    }
}

// over the space kept for it, on the text's baseline
private data class FormulaItem(override val key: Any, val formula: PlacedFormula) : OverlayItem {
    override fun measure(measurable: Measurable, layout: TextLayoutResult?, width: Int): Pair<Placeable, IntOffset?> {
        val style = formula.style
        val placeable = measurable.measure(Constraints.fixed(ceil(style.width).toInt(), ceil(style.height).toInt()))
        val position = layout?.let {
            val start = formula.placed.start
            val box = it.getBoundingBox(start)
            val baseline = it.getLineBaseline(it.getLineForOffset(start))
            // in the middle of its space - the placeholder of a formula in a heading is a bit wider
            IntOffset((box.left + (box.width - style.width) / 2).roundToInt(), (baseline - style.baseline).roundToInt())
        }
        return placeable to position
    }
}

// keyed by their source, not by where they are - typing above a picture must not load it again
@Composable
internal fun rememberOverlayItems(preview: LivePreviewText): List<OverlayItem> = remember(preview) {
    // the same source a second time gets a key of its own
    val occurrences = mutableMapOf<Any, Int>()
    fun keyOf(source: Any): Any {
        val occurrence = occurrences[source] ?: 0
        occurrences[source] = occurrence + 1
        return source to occurrence
    }

    val widgets = preview.widgets.map { placed ->
        val source = preview.source.substring(placed.decoration.start, placed.decoration.end)
        WidgetItem(keyOf("widget" to source), placed, source)
    }
    val formulas = preview.formulas.map { FormulaItem(keyOf("formula" to it.placed.decoration.latex), it) }
    widgets + formulas + preview.checkboxes.mapIndexed { index, placed -> CheckboxItem("checkbox" to index, placed) }
}

/**
 * Measures the text field (the first measurable) first, then the overlays with the layout it just made. A new
 * policy for every new preview makes the layout measure them again - and the text field with them, first.
 */
@Composable
internal fun rememberOverlayMeasurePolicy(preview: LivePreviewText, shownLayout: LayoutHolder): MeasurePolicy {
    // where each overlay was placed last - used in the (never expected) pass without a layout of this text
    val lastPositions = remember { mutableMapOf<Any, IntOffset>() }

    return remember(preview, shownLayout) {
        MeasurePolicy { measurables, constraints ->
            val field = measurables.first().measure(constraints)
            val layout = shownLayout.forText(preview)
            val overlays = measurables.drop(1).map { measurable ->
                val item = measurable.layoutId as OverlayItem
                val (placeable, position) = item.measure(measurable, layout, field.width)
                Triple(item, placeable, position)
            }
            layout(field.width, field.height) {
                field.place(0, 0)
                for ((item, placeable, position) in overlays) {
                    val placedAt = position ?: lastPositions[item.key] ?: continue
                    lastPositions[item.key] = placedAt
                    placeable.place(placedAt)
                }
            }
        }
    }
}

@Composable
internal fun PreviewOverlays(
    items: List<OverlayItem>,
    onWidgetHeight: (source: String, height: Int) -> Unit,
    onWidgetClick: (Widget) -> Unit,
    onWidgetSourceChange: (Widget, newSource: String) -> Unit,
    onFormulaClick: (InlineMath) -> Unit,
    onCheckboxClick: (TaskCheckbox) -> Unit,
) {
    for (item in items) {
        key(item.key) {
            when (item) {
                is WidgetItem -> DisableSelection {
                    Box(
                        modifier = Modifier
                            .layoutId(item)
                            .onSizeChanged { onWidgetHeight(item.source, it.height) }
                            // a click shows the source, to edit it
                            .pointerInput(item.placed.decoration) {
                                detectTapGestures { onWidgetClick(item.placed.decoration) }
                            }
                            .padding(vertical = WIDGET_VERTICAL_PADDING)
                    ) {
                        RenderedMarkdownBlock(
                            text = item.source,
                            onTextChange = { onWidgetSourceChange(item.placed.decoration, it) },
                        )
                    }
                }

                // a click shows the source, like on a widget
                is FormulaItem -> Box(
                    modifier = Modifier
                        .layoutId(item)
                        .pointerInput(item.formula.placed.decoration) {
                            detectTapGestures { onFormulaClick(item.formula.placed.decoration) }
                        }
                ) {
                    Latex(latex = item.formula.placed.decoration.latex, config = item.formula.style.config)
                }

                is CheckboxItem -> TaskCheckboxBox(
                    isChecked = item.placed.decoration.isChecked,
                    onClick = { onCheckboxClick(item.placed.decoration) },
                    modifier = Modifier.layoutId(item),
                )
            }
        }
    }
}

private val WIDGET_VERTICAL_PADDING = 4.dp

/** The heights the widgets' composables were measured with, by their source - the lines kept for them get them. */
@Stable
internal class WidgetHeights {
    private val measured = mutableStateMapOf<String, Int>()

    /** The heights measured so far - read in a composition, it recomposes it when one changes. */
    fun current(): Map<String, Int> = measured.toMap()

    fun record(source: String, height: Int) {
        // every edit of a widget's source adds a height - forget them once in a while
        if (measured.size > MAX_KNOWN_WIDGET_HEIGHTS) measured.clear()
        measured[source] = height
    }
}

private const val MAX_KNOWN_WIDGET_HEIGHTS = 200

/** The height of the line kept for [widget] in [source] - the measured one, or a guess until it is measured. */
internal fun widgetLineHeight(widget: Widget, source: String, knownHeights: Map<String, Int>, density: Density): TextUnit =
    with(density) {
        val widgetSource = source.substring(widget.start, widget.end)
        knownHeights[widgetSource]?.let { return it.toSp() }
        when (widget.kind) {
            WidgetKind.Table -> (widgetSource.count { it == '\n' } * 40 + 56).dp.toSp()
            WidgetKind.Image -> 200.dp.toSp()
            WidgetKind.Math -> 56.dp.toSp()
            WidgetKind.Rule -> 24.dp.toSp()
        }
    }

// the box of a task, drawn over the space the text keeps for it - a click checks it
@Composable
private fun TaskCheckboxBox(isChecked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val boxColor = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val checkColor = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier = modifier.clickable(interactionSource = null, indication = null, onClick = onClick)) {
        val side = minOf(size.width, size.height) * 0.75f
        val topLeft = Offset((size.width - side) / 2, (size.height - side) / 2)
        val radius = CornerRadius(side * 0.2f)

        if (isChecked) {
            drawRoundRect(boxColor, topLeft, Size(side, side), radius)
            val check = Path().apply {
                moveTo(topLeft.x + side * 0.22f, topLeft.y + side * 0.52f)
                lineTo(topLeft.x + side * 0.42f, topLeft.y + side * 0.72f)
                lineTo(topLeft.x + side * 0.78f, topLeft.y + side * 0.3f)
            }
            drawPath(check, checkColor, style = Stroke(width = side * 0.12f, cap = StrokeCap.Round))
        } else {
            drawRoundRect(boxColor, topLeft, Size(side, side), radius, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}
