package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LivePreviewTextTest {

    // every look gets a style of its own, so the tests can tell where each one went
    private val styles = PreviewStyles(
        look = { SpanStyle(fontFeatureSettings = it.name) },
        heading = { level -> SpanStyle(fontFeatureSettings = "h$level") to ParagraphStyle(lineHeight = (40 - level).sp) },
        alertTitle = { kind -> SpanStyle(fontFeatureSettings = "alert-${kind?.name}") },
    )

    /** The note shown with the cursor at [cursor] - null when the editor is not focused. */
    private fun preview(text: String, cursor: Int? = null, styles: PreviewStyles = this.styles): LivePreviewText =
        buildLivePreviewText(text, previewDecorations(text, parseNoteMarkdown(text), cursor?.let { TextRange(it) }), styles) {
            20.sp
        }

    // every formula can be drawn, in a line [lineHeight] tall
    private fun withFormulas(lineHeight: TextUnit = TextUnit.Unspecified) = PreviewStyles(
        look = styles.look,
        heading = styles.heading,
        formula = { _, _ -> FormulaStyle(30f, 20f, 15f, SpanStyle(fontFeatureSettings = "formula"), lineHeight) },
    )

    // the line breaks around paragraph styled lines are zero width spaces, the paragraphs break the lines there
    private val LivePreviewText.shown: String get() = transformed.text.text.replace(ZERO_WIDTH_SPACE, "\n")

    // the shown text that has [look]
    private fun LivePreviewText.withLook(look: String): List<String> =
        transformed.text.spanStyles
            .filter { it.item.fontFeatureSettings == look }
            .map { shown.substring(it.start, it.end) }

    // --- inline formatting ---

    @Test
    fun `bold text loses its markers away from the cursor`() {
        val preview = preview("a **bold** word")

        assertEquals("a bold word", preview.shown)
        assertEquals(listOf("bold"), preview.withLook("Bold"))
    }

    @Test
    fun `the markers come back dimmed where the cursor is`() {
        val preview = preview("a **bold** word", cursor = 5)

        assertEquals("a **bold** word", preview.shown)
        assertEquals(listOf("bold"), preview.withLook("Bold"))
        assertEquals(listOf("**", "**"), preview.withLook("Syntax"))
    }

    @Test
    fun `a cursor right after the formatting counts as in it`() {
        assertEquals("a **bold** word", preview("a **bold** word", cursor = 10).shown)
        assertEquals("a bold word", preview("a **bold** word", cursor = 11).shown)
    }

    @Test
    fun `italic and strikethrough and code are formatted too`() {
        val preview = preview("*it* ~~gone~~ `code`")

        assertEquals("it gone code", preview.shown)
        assertEquals(listOf("it"), preview.withLook("Italic"))
        assertEquals(listOf("gone"), preview.withLook("Strikethrough"))
        assertEquals(listOf("code"), preview.withLook("Code"))
    }

    @Test
    fun `formatting inside formatting keeps both`() {
        val preview = preview("**bold *both***")

        assertEquals("bold both", preview.shown)
        assertEquals(listOf("both"), preview.withLook("Italic"))
        assertEquals(listOf("bold both"), preview.withLook("Bold"))
    }

    @Test
    fun `a link shows only its text`() {
        val preview = preview("see [the docs](https://example.com \"title\") now")

        assertEquals("see the docs now", preview.shown)
        assertEquals(listOf("the docs"), preview.withLook("Link"))
    }

    @Test
    fun `a link opens its address from its shown text`() {
        val preview = preview("see [the docs](https://example.com \"title\") now")
        val link = preview.links.single()

        assertEquals("https://example.com", link.decoration.url)
        assertEquals("the docs", preview.shown.substring(link.start, link.end))
        assertFalse(link.decoration.isEdited)
    }

    @Test
    fun `a link being edited is marked so a click only puts the cursor there`() {
        assertTrue(preview("see [the docs](https://example.com) now", cursor = 6).links.single().decoration.isEdited)
    }

    @Test
    fun `addresses without a scheme open as web pages or e-mails`() {
        val urls = preview("<me@example.com> www.example.com <https://a.b>").links.map { it.decoration.url }

        assertEquals(listOf("mailto:me@example.com", "https://www.example.com", "https://a.b"), urls)
    }

    @Test
    fun `nothing is hidden inside code`() {
        assertEquals("**not bold**", preview("`**not bold**`").shown)
    }

    // --- lines ---

    @Test
    fun `a heading hides its hashes until the cursor is on it`() {
        val text = "# Title\ntext"

        assertEquals("Title\ntext", preview(text, cursor = 10).shown)
        assertEquals("# Title\ntext", preview(text, cursor = 3).shown)
        assertEquals(listOf("Title"), preview(text).withLook("h1"))
    }

    @Test
    fun `a heading without text keeps its hashes`() {
        assertEquals("#", preview("#").shown)
    }

    @Test
    fun `list bullets become dots - a different one for every level`() {
        assertEquals("• one\n  ◦ two", preview("- one\n  - two").shown)
    }

    @Test
    fun `an item indented by four spaces keeps its indentation and only its bullet changes`() {
        // the parser gives the nested item's bullet token as "  - ", the first spaces are the parent's indentation
        assertEquals("• parent\n    ◦ child", preview("- parent\n    - child").shown)
        assertEquals("• parent\n    $CHECKBOX_PLACEHOLDER task", preview("- parent\n    - [ ] task").shown)
        // a number is dimmed without the indentation in front of it
        assertEquals(listOf("1. "), preview("- parent\n    1. number").withLook("Syntax"))
    }

    @Test
    fun `the wrapped lines of a list item start where its text starts`() {
        val text = "- parent\n    - child\n- [ ] task\n1. number"
        val prefixes = mutableListOf<String>()
        // as wide as the prefix is long, in sp
        val measuring = PreviewStyles(styles.look, styles.heading, listIndent = { prefix -> prefixes += prefix; prefix.length.sp })
        val preview = buildLivePreviewText(text, previewDecorations(text, parseNoteMarkdown(text), null), measuring) { 20.sp }

        // measured as shown: the bullets as dots, the task's brackets as its box
        assertEquals(listOf("• ", "    ◦ ", "$CHECKBOX_PLACEHOLDER ", "1. "), prefixes)
        assertEquals(
            listOf(2.sp, 6.sp, 2.sp, 3.sp),
            preview.transformed.text.paragraphStyles.map { it.item.textIndent?.restLine },
        )
    }

    @Test
    fun `a numbered list shows the numbers its items have in it - counted from the first one`() {
        // Enter in a list continues it with "1. "
        assertEquals("1. a\n2. b\n3. c", preview("1. a\n1. b\n1. c").shown)
        assertEquals("3. a\n4. b", preview("3. a\n3. b").shown)
        assertEquals("1. a\n    1. nested\n2. b", preview("1. a\n    1. nested\n1. b").shown)
    }

    @Test
    fun `the number with the cursor right at it is the written one`() {
        assertEquals("1. a\n1. b", preview("1. a\n1. b", cursor = 7).shown)
        assertEquals("1. a\n2. b", preview("1. a\n1. b", cursor = 8).shown)
    }

    @Test
    fun `the bullet with the cursor right at it stays a dash`() {
        assertEquals("- one", preview("- one", cursor = 1).shown)
        assertEquals("• one", preview("- one", cursor = 2).shown)
    }

    @Test
    fun `a task is a box - a done one is crossed out`() {
        val preview = preview("- [ ] milk\n- [x] bread")

        assertEquals("$CHECKBOX_PLACEHOLDER milk\n$CHECKBOX_PLACEHOLDER bread", preview.shown)
        assertEquals(listOf(false, true), preview.checkboxes.map { it.decoration.isChecked })
        assertEquals(listOf(2, 13), preview.checkboxes.map { it.decoration.start })
        assertEquals(listOf("bread"), preview.withLook("DoneTask"))
    }

    @Test
    fun `a quote shows a bar instead of its markers`() {
        val preview = preview("> quoted\n> lines")

        assertEquals("${QUOTE_PLACEHOLDER}quoted\n${QUOTE_PLACEHOLDER}lines", preview.shown)
        assertEquals(1, preview.quoteBars.size)
        assertEquals(0, preview.quoteBars.single().anchor)
    }

    @Test
    fun `a GitHub alert is a quote with a title`() {
        val preview = preview("> [!TIP]\n> yoyo")

        assertEquals("${QUOTE_PLACEHOLDER}💡 Tip\n${QUOTE_PLACEHOLDER}yoyo", preview.shown)
        assertEquals(listOf("💡 Tip"), preview.withLook("alert-Tip"))
        assertEquals(AlertKind.Tip, preview.quoteBars.single().decoration.alert)
        assertEquals("> [!TIP]\n> yoyo", preview("> [!TIP]\n> yoyo", cursor = 4).shown)
    }

    @Test
    fun `every alert type has its emoji - another one is an ordinary quote like on GitHub`() {
        val titles = listOf("NOTE", "TIP", "IMPORTANT", "WARNING", "CAUTION", "OTHER").map { type ->
            preview("> [!$type]\n> text").shown.substringBefore('\n').removePrefix(QUOTE_PLACEHOLDER)
        }

        assertEquals(listOf("ℹ️ Note", "💡 Tip", "❗ Important", "⚠️ Warning", "🛑 Caution", "[!OTHER]"), titles)
    }

    @Test
    fun `code is highlighted in its language - also with its fence lines hidden`() {
        val text = "text\n\n```kotlin\nval x = 1\n```"
        val asked = mutableListOf<Pair<String, String?>>()
        // "val" as a keyword
        val highlighting = PreviewStyles(styles.look, styles.heading, codeHighlights = { code, language ->
            asked += code to language
            listOf(StyledRange(0, 3, SpanStyle(fontFeatureSettings = "keyword")))
        })

        for (cursor in listOf(null, 20)) {
            val preview = buildLivePreviewText(
                text, previewDecorations(text, parseNoteMarkdown(text), cursor?.let { TextRange(it) }), highlighting,
            ) { 20.sp }
            assertEquals(listOf("val"), preview.withLook("keyword"))
        }
        assertEquals("val x = 1" to "kotlin", asked.first())
    }

    @Test
    fun `code keeps its lines - the fence lines are hidden away from the cursor`() {
        val text = "text\n\n```kotlin\nval x = 1\n```\n\nmore"

        assertEquals("text\n\nval x = 1\n\nmore", preview(text).shown)
        assertEquals(text, preview(text, cursor = 20).shown)
        assertEquals(1, preview(text).codeBackgrounds.size)
    }

    // --- widgets ---

    @Test
    fun `tables and images and maths and rules are drawn by composables away from the cursor`() {
        val text = "| a |\n|---|\n| 1 |\n\n![photo](images/a.png)\n\n$$\nx^2\n$$\n\n---"
        val preview = preview(text)

        assertEquals(
            listOf(WidgetKind.Table, WidgetKind.Image, WidgetKind.Math, WidgetKind.Rule),
            preview.widgets.map { it.decoration.kind },
        )
        // each one keeps a line of its own
        assertEquals(List(4) { WIDGET_PLACEHOLDER }.joinToString("\n\n"), preview.shown)
        assertTrue('\n' !in preview.transformed.text.text, "no line break next to a paragraph")
        assertEquals(4, preview.transformed.text.paragraphStyles.size)
    }

    @Test
    fun `a note ending with a line break after a widget keeps its empty last line`() {
        val text = "text\n\n![photo](images/a.png)\n"
        val preview = preview(text)
        val paragraph = preview.transformed.text.paragraphStyles.single()
        val emptyLastLine = preview.offsetMapping.originalToTransformed(text.length)

        // the widget's paragraph ends before the zero width space of the last line break
        assertEquals(paragraph.end, emptyLastLine - 1)
        assertEquals(WIDGET_PLACEHOLDER, preview.transformed.text.text.substring(paragraph.start, paragraph.end))
    }

    @Test
    fun `the cursor in a widget shows its source`() {
        val text = "text\n\n| a |\n|---|\n| 1 |"
        val preview = preview(text, cursor = 8)

        assertTrue(preview.widgets.isEmpty())
        assertEquals(text, preview.shown)
    }

    @Test
    fun `an image inside text keeps its source`() {
        val text = "look ![photo](images/a.png) here"

        assertEquals(text, preview(text).shown)
    }

    @Test
    fun `maths inside text is drawn as a formula away from the cursor`() {
        val preview = preview("area \$x^2\$ here", styles = withFormulas())

        assertEquals("area $MATH_PLACEHOLDER here", preview.shown)
        assertEquals(listOf(MATH_PLACEHOLDER), preview.withLook("formula"))
        assertEquals("x^2", preview.formulas.single().placed.decoration.latex)
        assertEquals(5, preview.formulas.single().placed.start)
    }

    @Test
    fun `the cursor at maths shows its source`() {
        val preview = preview("area \$x^2\$ here", cursor = 6, styles = withFormulas())

        assertEquals("area \$x^2\$ here", preview.shown)
        assertEquals(listOf("\$x^2\$"), preview.withLook("Math"))
        assertTrue(preview.formulas.isEmpty())
    }

    @Test
    fun `maths that cannot be drawn keeps its source`() {
        val preview = preview("area \$x^2\$ here")

        assertEquals("area \$x^2\$ here", preview.shown)
        assertEquals(listOf("\$x^2\$"), preview.withLook("Math"))
    }

    @Test
    fun `a formula taller than its line makes the line taller - never lower`() {
        val tall = preview("a \$\\frac{1}{2}\$ b", styles = withFormulas(lineHeight = 50.sp))
        assertEquals(listOf(50.sp), tall.transformed.text.paragraphStyles.map { it.item.lineHeight })

        // the heading's line is taller already
        val heading = preview("# a \$x\$ b", styles = withFormulas(lineHeight = 30.sp))
        assertEquals(listOf(39.sp), heading.transformed.text.paragraphStyles.map { it.item.lineHeight })
    }

    @Test
    fun `a click on a widget's line goes to the widget's start`() {
        val text = "text\n\n![photo](images/a.png)\n\nmore"
        val preview = preview(text)
        val widgetLine = preview.widgets.single().start

        assertEquals(6, preview.offsetMapping.transformedToOriginal(widgetLine))
        assertEquals(text.indexOf("more"), preview.offsetMapping.transformedToOriginal(preview.shown.indexOf("more")))
    }

    // --- offsets ---

    @Test
    fun `offsets map between the source and the shown text both ways`() {
        val text = "# Title\n\nsome **bold** and [a link](url)\n\n- [ ] task\n> quote\n\n```\ncode\n```\n\n| a |\n|---|\n\nend"
        val preview = preview(text)
        val mapping = preview.offsetMapping

        var previous = 0
        for (offset in 0..text.length) {
            val shown = mapping.originalToTransformed(offset)
            assertTrue(shown in 0..preview.shown.length, "offset $offset shown at $shown")
            assertTrue(shown >= previous, "offsets keep their order")
            previous = shown
        }
        for (offset in 0..preview.shown.length) {
            assertTrue(mapping.transformedToOriginal(offset) in 0..text.length)
        }

        // the text that is shown maps to itself
        val word = text.indexOf("bold")
        assertEquals(word, mapping.transformedToOriginal(mapping.originalToTransformed(word)))
        assertEquals("end", preview.shown.substring(mapping.originalToTransformed(text.indexOf("end"))))
    }

    @Test
    fun `an empty note is shown as it is`() {
        assertEquals("", preview("").shown)
    }
}
