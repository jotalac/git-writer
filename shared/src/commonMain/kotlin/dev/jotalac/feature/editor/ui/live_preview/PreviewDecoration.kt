package dev.jotalac.feature.editor.ui.live_preview

/*
* The decorations below say how a range of the note is shown. DecorationCollector.kt finds them in the markdown
* syntax tree, LivePreviewText.kt turns them into the text the text field shows.
* */

/** How a part of the text looks. */
internal enum class TextLook { Bold, Italic, Strikethrough, Highlight, Code, Link, Syntax, DoneTask, Math, Monospace }

/** The GitHub alerts (`> [!TIP]`) - shown with an emoji and the type's color, like GitHub shows them. */
internal enum class AlertKind(val emoji: String) {
    Note("ℹ️"),
    Tip("💡"),
    Important("❗"),
    Warning("⚠️"),
    Caution("🛑");

    companion object {
        fun of(name: String): AlertKind? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

internal enum class WidgetKind { Table, Image, Math, Rule }

/** One change of how the range [start, end) of the note is shown. */
internal sealed interface PreviewDecoration {
    val start: Int
    val end: Int
}

/** A decoration that shows other text in place of its range - and so moves the text after it. */
internal sealed interface ReplacingDecoration : PreviewDecoration {
    val replacement: String
}

/** The source is shown as [replacement] - an empty one hides it (the syntax around formatted text). */
internal data class Replacement(
    override val start: Int,
    override val end: Int,
    override val replacement: String,
) : ReplacingDecoration

/** Whole lines drawn by a composable, see [WidgetKind]. The text keeps one line as tall as the composable. */
internal data class Widget(override val start: Int, override val end: Int, val kind: WidgetKind) : ReplacingDecoration {
    override val replacement: String get() = WIDGET_PLACEHOLDER
}

/** The `[ ]` of a task, shown as a box that can be clicked. */
internal data class TaskCheckbox(override val start: Int, override val end: Int, val isChecked: Boolean) :
    ReplacingDecoration {
    override val replacement: String get() = CHECKBOX_PLACEHOLDER
}

/** Maths inside text (`$x^2$`), drawn as a formula over the space kept for it - [latex] is without the `$`. */
internal data class InlineMath(override val start: Int, override val end: Int, val latex: String) : ReplacingDecoration {
    override val replacement: String get() = MATH_PLACEHOLDER
}

/** The source in another [look]. */
internal data class Look(override val start: Int, override val end: Int, val look: TextLook) : PreviewDecoration

/** A heading line - drawn in the heading's size. */
internal data class HeadingLine(override val start: Int, override val end: Int, val level: Int) : PreviewDecoration

/** The title of a GitHub alert - in the color of its [kind], or of the theme for a title of another kind. */
internal data class AlertTitle(override val start: Int, override val end: Int, val kind: AlertKind?) : PreviewDecoration

/**
 * A quote, drawn with a bar beside it - at the quote's first `>` ([markerStart]), so that nested quotes get their
 * own bar. The bar of a GitHub [alert] has the alert's color.
 */
internal data class QuoteBar(
    override val start: Int,
    override val end: Int,
    val markerStart: Int,
    val alert: AlertKind? = null,
) : PreviewDecoration

/** A line of a quote, [depth] quotes deep - its wrapped lines start after their bars. */
internal data class QuoteLine(override val start: Int, override val end: Int, val depth: Int) : PreviewDecoration

/** The first line of a list item - its wrapped lines start under its text, which starts at [contentStart]. */
internal data class ListLine(override val start: Int, override val end: Int, val contentStart: Int) : PreviewDecoration

/**
 * The text of a link, which opens [url] when clicked - while [isEdited], its syntax is shown and a click only puts
 * the cursor there (Ctrl/Cmd+click still opens it).
 */
internal data class LinkTarget(
    override val start: Int,
    override val end: Int,
    val url: String,
    val isEdited: Boolean,
) : PreviewDecoration

/** The code of a code block, in [language] as written after its fence - highlighted like the rendered notes. */
internal data class CodeContent(override val start: Int, override val end: Int, val language: String?) : PreviewDecoration

/** Code lines, drawn on a background. */
internal data class CodeBackground(override val start: Int, override val end: Int) : PreviewDecoration

// one character - the line of a widget gets the composable's height
internal const val WIDGET_PLACEHOLDER = " "

// an em space, as wide as the text is tall - the box of a task is drawn over it
internal const val CHECKBOX_PLACEHOLDER = " "

// a no-break space stretched to the formula's width - it never breaks a line, and never hangs off its end
internal const val MATH_PLACEHOLDER = "\u00A0"

// a `>` and its space become an em space, which leaves room for the quote's bar
internal const val QUOTE_PLACEHOLDER = " "

// a different bullet for every nesting level, the deepest one repeats - like the rendered lists
internal val LIST_BULLETS = listOf("•", "◦", "▪", "▫")
