package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.measure.LatexDimensions
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import dev.jotalac.feature.editor.ui.utils.getHeaderFontSize
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes

/** How the decorations are drawn - from the app's theme, see [rememberPreviewStyles]. */
internal class PreviewStyles(
    val look: (TextLook) -> SpanStyle,
    val heading: (level: Int) -> Pair<SpanStyle, ParagraphStyle>,
    val alertTitle: (AlertKind?) -> SpanStyle = { SpanStyle() },
    /** The syntax highlighting of [code] in [language] - its ranges are offsets in [code]. */
    val codeHighlights: (code: String, language: String?) -> List<StyledRange> = { _, _ -> emptyList() },
    /** How wide [prefix] is shown - the indentation and the bullet of a list item, its wrapped lines start there. */
    val listIndent: (prefix: String) -> TextUnit = { TextUnit.Unspecified },
    /**
     * How [latex] is drawn inside text, or inside a heading of [headingLevel] - null when it cannot be drawn, it then
     * keeps its source.
     */
    val formula: (latex: String, headingLevel: Int?) -> FormulaStyle? = { _, _ -> null },
)

/** A [style] for [start, end). */
internal data class StyledRange(val start: Int, val end: Int, val style: SpanStyle)

/**
 * A formula [width] x [height] px, which sits on the text's baseline [baseline] px below its top. The text keeps
 * its space with a [MATH_PLACEHOLDER] in [placeholder] style, on a line [lineHeight] tall - unspecified when the
 * formula fits the text's own line. It is drawn with the [config] it was measured with.
 */
internal data class FormulaStyle(
    val width: Float,
    val height: Float,
    val baseline: Float,
    val placeholder: SpanStyle,
    val lineHeight: TextUnit = TextUnit.Unspecified,
    val config: LatexConfig = LatexConfig(),
)

/** The styles of the decorations in the app's theme, for text in [textStyle]. */
@Composable
internal fun rememberPreviewStyles(textStyle: TextStyle): PreviewStyles {
    val colors = MaterialTheme.colorScheme
    val headingStyles = (1..6).map { getHeaderFontSize(it) }
    // the highlighting theme follows the app's theme, not the system's
    val isDarkTheme = colors.background.luminance() < 0.5f
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // the formulas in the text's size and color, on no background of their own (code, a highlight)
    val formulaConfig = LatexConfig(fontSize = textStyle.fontSize, theme = LatexTheme.light(color = colors.onSurface))
    val latexMeasurer = rememberLatexMeasurer(formulaConfig)

    return remember(colors, headingStyles, isDarkTheme, textStyle, textMeasurer, density, formulaConfig, latexMeasurer) {
        val monospace = SpanStyle(fontFamily = FontFamily.Monospace)
        // highlighted again only when the code changes - the preview is built again on every cursor move
        val highlightedCode = mutableMapOf<Pair<String, String?>, List<StyledRange>>()
        // measured in the text's own font, so the wrapped lines start exactly under the item's first letter
        val listIndents = mutableMapOf<String, TextUnit>()
        // measured for the text and for every heading level the first time a formula is in it
        val formulaSpaces = mutableMapOf<Int?, FormulaSpace>()

        PreviewStyles(
            look = { look ->
                when (look) {
                    TextLook.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
                    TextLook.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
                    TextLook.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    TextLook.Highlight -> SpanStyle(background = colors.primary.copy(alpha = 0.4f))
                    TextLook.Code -> monospace.copy(background = colors.surfaceContainerHighest)
                    TextLook.Link -> SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)
                    TextLook.Syntax -> SpanStyle(color = colors.onSurface.copy(alpha = 0.35f))
                    TextLook.DoneTask -> SpanStyle(
                        color = colors.onSurface.copy(alpha = 0.5f),
                        textDecoration = TextDecoration.LineThrough,
                    )
                    TextLook.Math -> monospace.copy(color = colors.tertiary)
                    TextLook.Monospace -> monospace
                }
            },
            heading = { level ->
                val style = headingStyles[(level - 1).coerceIn(0, headingStyles.lastIndex)]
                style.toSpanStyle() to ParagraphStyle(lineHeight = style.lineHeight)
            },
            alertTitle = { kind -> SpanStyle(fontWeight = FontWeight.Bold, color = kind?.color ?: colors.primary) },
            codeHighlights = { code, language ->
                if (highlightedCode.size > MAX_HIGHLIGHTED_CODE) highlightedCode.clear()
                highlightedCode.getOrPut(code to language) { highlightCode(code, language, isDarkTheme) }
            },
            listIndent = { prefix ->
                listIndents.getOrPut(prefix) {
                    with(density) { textMeasurer.measure(prefix, textStyle).size.width.toSp() }
                }
            },
            formula = { latex, headingLevel ->
                val space = formulaSpaces.getOrPut(headingLevel) {
                    val style = headingLevel?.let { textStyle.merge(headingStyles[(it - 1).coerceIn(0, headingStyles.lastIndex)]) }
                    FormulaSpace.measure(textMeasurer, style ?: textStyle, formulaConfig)
                }
                // the measurer keeps the sizes it measured
                latexMeasurer.measure(latex, space.config)?.let { space.styleFor(it, density) }
            },
        )
    }
}

private const val MAX_HIGHLIGHTED_CODE = 100

/**
 * The space a line of text in its own style has for a formula drawn with [config] (in the text's size):
 * [placeholderWidth] is how wide a [MATH_PLACEHOLDER] is without any stretching, [lineHeight] how tall the line is,
 * with its baseline [baseline] below its top.
 */
private class FormulaSpace(
    val config: LatexConfig,
    val placeholderWidth: Float,
    val lineHeight: Float,
    val baseline: Float,
) {

    fun styleFor(formula: LatexDimensions, density: Density): FormulaStyle = with(density) {
        // what is drawn above and below the baseline - the padding around it may go over the lines next to it
        val above = formula.contentBaselinePx
        val below = formula.contentHeightPx - formula.contentBaselinePx
        // a taller line grows by the same on both sides, see FORMULA_LINE_HEIGHT_STYLE
        val grow = 2 * maxOf(above - baseline, below - (lineHeight - baseline), 0f)

        FormulaStyle(
            width = formula.widthPx,
            height = formula.heightPx,
            baseline = formula.baselinePx,
            placeholder = SpanStyle(
                color = Color.Transparent,
                letterSpacing = (formula.widthPx - placeholderWidth).coerceAtLeast(0f).toSp(),
            ),
            lineHeight = if (grow > 0f) (lineHeight + grow + FORMULA_LINE_PADDING.toPx()).toSp() else TextUnit.Unspecified,
            config = config,
        )
    }

    companion object {
        fun measure(textMeasurer: TextMeasurer, textStyle: TextStyle, config: LatexConfig): FormulaSpace {
            val line = textMeasurer.measure("x", textStyle.copy(lineHeightStyle = FORMULA_LINE_HEIGHT_STYLE))
            val placeholder = textMeasurer.measure(MATH_PLACEHOLDER, textStyle)
            return FormulaSpace(
                config = config.copy(fontSize = textStyle.fontSize),
                placeholderWidth = placeholder.getHorizontalPosition(MATH_PLACEHOLDER.length, usePrimaryDirection = true),
                lineHeight = line.size.height.toFloat(),
                baseline = line.firstBaseline,
            )
        }
    }
}

/** A line made taller for a formula keeps its text in the middle. */
internal val FORMULA_LINE_HEIGHT_STYLE = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private val FORMULA_LINE_PADDING = 4.dp

/**
 * The highlighting of [code] by the Highlights library - the one the markdown library highlights its code blocks
 * with, in the same theme, so code looks the same while edited and rendered (and in the PDF export).
 */
private fun highlightCode(code: String, language: String?, isDarkTheme: Boolean): List<StyledRange> {
    val syntaxLanguage = language?.let { SyntaxLanguage.getByName(it.lowercase()) } ?: SyntaxLanguage.DEFAULT
    val highlights = Highlights.Builder()
        .code(code)
        .language(syntaxLanguage)
        .theme(SyntaxThemes.atom(darkMode = isDarkTheme))
        .build()
        .getHighlights()

    return highlights.map { highlight ->
        val style = when (highlight) {
            is ColorHighlight -> SpanStyle(color = Color(0xFF000000 or highlight.rgb.toLong()))
            is BoldHighlight -> SpanStyle(fontWeight = FontWeight.Bold)
        }
        StyledRange(highlight.location.start, highlight.location.end, style)
    }
}

/** GitHub's color of the alert type - readable on light and dark backgrounds. */
internal val AlertKind.color: Color
    get() = when (this) {
        AlertKind.Note -> Color(0xFF4493F8)
        AlertKind.Tip -> Color(0xFF3FB950)
        AlertKind.Important -> Color(0xFFAB7DF8)
        AlertKind.Warning -> Color(0xFFD29922)
        AlertKind.Caution -> Color(0xFFF85149)
    }
