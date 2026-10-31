package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified

/**
 * A decoration drawn outside the text, at the range [start, end) of the shown (transformed) text. [anchor] is
 * where in it the drawing starts - the first `>` of a quote, for its bar.
 */
internal data class PlacedDecoration<T : PreviewDecoration>(
    val decoration: T,
    val start: Int,
    val end: Int,
    val anchor: Int = start,
)

/** A formula drawn over its [placed] space, in its [style]. */
internal data class PlacedFormula(val placed: PlacedDecoration<InlineMath>, val style: FormulaStyle)

/** The text the text field shows for a note, see [buildLivePreviewText]. */
internal class LivePreviewText(
    /** The source text this was built for. */
    val source: String,
    val transformed: TransformedText,
    val widgets: List<PlacedDecoration<Widget>>,
    val checkboxes: List<PlacedDecoration<TaskCheckbox>>,
    val quoteBars: List<PlacedDecoration<QuoteBar>>,
    val codeBackgrounds: List<PlacedDecoration<CodeBackground>>,
    val links: List<PlacedDecoration<LinkTarget>>,
    val formulas: List<PlacedFormula>,
) {
    val offsetMapping: OffsetMapping get() = transformed.offsetMapping
}

/**
 * The text [source] shown with its [decorations]: their replacements in place of the source, in their styles.
 * [widgetLineHeight] is how tall the line kept for a widget's composable has to be.
 */
internal fun buildLivePreviewText(
    source: String,
    decorations: List<PreviewDecoration>,
    styles: PreviewStyles,
    widgetLineHeight: (Widget) -> TextUnit,
): LivePreviewText {
    val headings = decorations.filterIsInstance<HeadingLine>()
    val formulaStyles = decorations.filterIsInstance<InlineMath>()
        .mapNotNull { math ->
            val heading = headings.firstOrNull { math.start >= it.start && math.end <= it.end }
            styles.formula(math.latex, heading?.level)?.let { math to it }
        }
        .toMap()
    // maths that cannot be drawn keeps its source
    val shownDecorations = decorations.map {
        if (it is InlineMath && it !in formulaStyles) Look(it.start, it.end, TextLook.Math) else it
    }

    val decorationReplacements = withoutOverlaps(shownDecorations.filterIsInstance<ReplacingDecoration>())
    val formulas = decorationReplacements.filterIsInstance<InlineMath>().associateWith { formulaStyles.getValue(it) }
    val paragraphLines = paragraphLines(source, shownDecorations, decorationReplacements, formulas, styles, widgetLineHeight)
    val replacements = withoutOverlaps(decorationReplacements + lineBreakReplacements(source, paragraphLines))
    val mapping = ReplacementOffsetMapping(source.length, replacements)

    val text = buildAnnotatedString {
        append(shownText(source, replacements, 0, source.length))
        addSpanStyles(source, shownDecorations, formulas, styles, mapping)
        addParagraphStyles(source, paragraphLines, mapping)
    }

    return LivePreviewText(
        source = source,
        transformed = TransformedText(text, mapping),
        widgets = replacements.placed(mapping),
        checkboxes = replacements.placed(mapping),
        quoteBars = decorations.filterIsInstance<QuoteBar>().map {
            PlacedDecoration(
                it,
                mapping.originalToTransformed(it.start),
                mapping.originalToTransformed(it.end),
                anchor = mapping.originalToTransformed(it.markerStart),
            )
        },
        codeBackgrounds = decorations.placed(mapping),
        links = decorations.placed(mapping),
        formulas = replacements.placed<InlineMath>(mapping).map { PlacedFormula(it, formulas.getValue(it.decoration)) },
    )
}

/** A line shown as a paragraph of its own, in [style]. */
private data class ParagraphLine(val start: Int, val end: Int, val style: ParagraphStyle)

// the lines that get a paragraph style - a heading's line height, a widget's height, a tall formula's line, the
// indent of a quote or a list item - by their start
private fun paragraphLines(
    source: String,
    decorations: List<PreviewDecoration>,
    replacements: List<ReplacingDecoration>,
    formulas: Map<InlineMath, FormulaStyle>,
    styles: PreviewStyles,
    widgetLineHeight: (Widget) -> TextUnit,
): Collection<ParagraphLine> {
    val lines = mutableMapOf<Int, ParagraphLine>()
    fun add(decoration: PreviewDecoration, style: ParagraphStyle) {
        val start = source.lineStartAt(decoration.start)
        val line = lines[start]
        lines[start] = ParagraphLine(
            start = start,
            end = maxOf(source.lineEndAt(decoration.end), line?.end ?: 0),
            style = line?.style?.merge(style) ?: style,
        )
    }

    for (decoration in decorations) {
        when (decoration) {
            is HeadingLine -> add(decoration, styles.heading(decoration.level).second)
            is QuoteLine -> add(decoration, ParagraphStyle(textIndent = TextIndent(restLine = decoration.depth.em)))
            is Widget -> if (decoration in replacements) add(decoration, widgetParagraphStyle(widgetLineHeight(decoration)))
            else -> Unit
        }
    }
    // a formula taller than its line makes the line taller - never lower than it is already (a heading, a taller
    // formula on the same line)
    for ((math, formula) in formulas) {
        val lineHeight = lines[source.lineStartAt(math.start)]?.style?.lineHeight ?: TextUnit.Unspecified
        if (formula.lineHeight.isSpecified && (!lineHeight.isSpecified || formula.lineHeight.value > lineHeight.value)) {
            add(math, ParagraphStyle(lineHeight = formula.lineHeight, lineHeightStyle = FORMULA_LINE_HEIGHT_STYLE))
        }
    }
    // after the quotes: a list item in a quote is measured with the quote's bar space in front of it, so its indent
    // replaces the quote's
    for (line in decorations.filterIsInstance<ListLine>()) {
        val indent = styles.listIndent(shownText(source, replacements, line.start, line.contentStart))
        if (indent.isSpecified) add(line, ParagraphStyle(textIndent = TextIndent(restLine = indent)))
    }
    return lines.values
}

// the line keeps the widget's height, the composable is drawn from its top
private fun widgetParagraphStyle(lineHeight: TextUnit) = ParagraphStyle(
    lineHeight = lineHeight,
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Top, trim = LineHeightStyle.Trim.None),
)

// A paragraph style makes its range a paragraph of its own, and a line break right next to it would add an empty
// line - so the line breaks around those lines are shown as zero width spaces (one character for one), inside the
// paragraph before them: the paragraphs break the lines there. The cursor at the end of a line stays on that line.
private fun lineBreakReplacements(source: String, lines: Collection<ParagraphLine>): List<Replacement> =
    lines.flatMap { line ->
        listOfNotNull(
            Replacement(line.start - 1, line.start, ZERO_WIDTH_SPACE).takeIf { line.start > 0 },
            Replacement(line.end, line.end + 1, ZERO_WIDTH_SPACE).takeIf { line.end < source.length },
        )
    }

private fun AnnotatedString.Builder.addSpanStyles(
    source: String,
    decorations: List<PreviewDecoration>,
    formulas: Map<InlineMath, FormulaStyle>,
    styles: PreviewStyles,
    mapping: OffsetMapping,
) {
    // [start, end) of the source, where it is shown
    fun addSourceStyle(style: SpanStyle, start: Int, end: Int) {
        val shownStart = mapping.originalToTransformed(start)
        val shownEnd = mapping.originalToTransformed(end)
        if (shownEnd > shownStart) addStyle(style, shownStart, shownEnd)
    }

    for (decoration in decorations) {
        when (decoration) {
            is Look -> addSourceStyle(styles.look(decoration.look), decoration.start, decoration.end)
            is HeadingLine -> addSourceStyle(styles.heading(decoration.level).first, decoration.start, decoration.end)
            is AlertTitle -> addSourceStyle(styles.alertTitle(decoration.kind), decoration.start, decoration.end)
            // the placeholder stretched to the formula's width
            is InlineMath -> formulas[decoration]?.let { addSourceStyle(it.placeholder, decoration.start, decoration.end) }
            // nothing in the code is replaced, its highlighting maps over as it is
            is CodeContent -> {
                val code = source.substring(decoration.start, decoration.end)
                for (highlight in styles.codeHighlights(code, decoration.language)) {
                    val start = decoration.start + highlight.start.coerceIn(0, code.length)
                    val end = decoration.start + highlight.end.coerceIn(0, code.length)
                    addSourceStyle(highlight.style, start, end)
                }
            }
            else -> Unit
        }
    }
}

private fun AnnotatedString.Builder.addParagraphStyles(
    source: String,
    lines: Collection<ParagraphLine>,
    mapping: OffsetMapping,
) {
    for (line in lines) {
        val start = mapping.originalToTransformed(line.start)
        // with the zero width space of the line break after it - unless the note ends with that line break: the
        // space then is the empty last line, where the cursor goes after it
        val end = mapping.originalToTransformed(if (line.end + 1 < source.length) line.end + 1 else line.end)
        if (end > start) addStyle(line.style, start, end)
    }
}

// [from, to) of the source as it is shown - with the replacements (sorted, not overlapping) in it
private fun shownText(source: String, replacements: List<ReplacingDecoration>, from: Int, to: Int): String =
    buildString {
        var position = from
        for (replacement in replacements) {
            if (replacement.end <= from || replacement.start >= to) continue
            append(source, position, maxOf(position, replacement.start))
            append(replacement.replacement)
            position = minOf(to, replacement.end)
        }
        if (position < to) append(source, position, to)
    }

// sorted, and never two over the same text - the first one wins
private fun withoutOverlaps(replacements: List<ReplacingDecoration>): List<ReplacingDecoration> =
    replacements.sortedBy { it.start }.fold(mutableListOf()) { kept, replacement ->
        if (kept.lastOrNull()?.let { it.end > replacement.start } != true) kept += replacement
        kept
    }

// the decorations of type [T], where they are in the shown text
private inline fun <reified T : PreviewDecoration> List<PreviewDecoration>.placed(
    mapping: OffsetMapping,
): List<PlacedDecoration<T>> = filterIsInstance<T>().map {
    PlacedDecoration(it, mapping.originalToTransformed(it.start), mapping.originalToTransformed(it.end))
}

internal const val ZERO_WIDTH_SPACE = "​"
