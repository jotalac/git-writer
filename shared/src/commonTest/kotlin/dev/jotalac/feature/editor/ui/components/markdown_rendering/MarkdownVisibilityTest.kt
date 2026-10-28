package dev.jotalac.feature.editor.ui.components.markdown_rendering

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownVisibilityTest {

    /** Whether [blockText], parsed like the editor parses it, draws anything. */
    private fun drawsAnything(blockText: String): Boolean =
        MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(blockText).drawsAnything(blockText)

    // --- drawn

    @Test
    fun `plain text is drawn`() {
        assertTrue(drawsAnything("some text"))
    }

    @Test
    fun `formatted text is drawn`() {
        assertTrue(drawsAnything("**bold** and _italic_ and ~~gone~~ and ==marked=="))
    }

    @Test
    fun `punctuation alone is drawn`() {
        assertTrue(drawsAnything("(:)"))
    }

    @Test
    fun `a star that is not emphasis is drawn`() {
        assertTrue(drawsAnything("**"))
    }

    @Test
    fun `a link with text is drawn`() {
        assertTrue(drawsAnything("[docs](https://example.com)"))
    }

    @Test
    fun `an empty heading is drawn`() {
        assertTrue(drawsAnything("##"))
    }

    @Test
    fun `an empty list item is drawn`() {
        assertTrue(drawsAnything("- "))
    }

    @Test
    fun `an empty code fence is drawn`() {
        assertTrue(drawsAnything("```\n```"))
    }

    @Test
    fun `a horizontal rule is drawn`() {
        assertTrue(drawsAnything("---"))
    }

    @Test
    fun `an image without alt text is drawn`() {
        assertTrue(drawsAnything("![](images/a.png)"))
    }

    @Test
    fun `maths is drawn`() {
        assertTrue(drawsAnything("$$ x^2 $$"))
    }

    @Test
    fun `a quote with text is drawn`() {
        assertTrue(drawsAnything("> quoted"))
    }

    @Test
    fun `a link definition next to text is drawn`() {
        assertTrue(drawsAnything("[a]: https://example.com\n\nsome text"))
    }

    // --- not drawn

    @Test
    fun `a link without text is not drawn`() {
        assertFalse(drawsAnything("[](https://example.com)"))
    }

    @Test
    fun `a link definition alone is not drawn`() {
        assertFalse(drawsAnything("[a]: https://example.com"))
    }

    @Test
    fun `an html comment is not drawn`() {
        assertFalse(drawsAnything("<!-- hidden -->"))
    }

    @Test
    fun `an html block is not drawn`() {
        assertFalse(drawsAnything("<div>\n</div>"))
    }

    @Test
    fun `an empty quote is not drawn`() {
        assertFalse(drawsAnything(">"))
    }

    @Test
    fun `a link with only html tags as text is not drawn`() {
        assertFalse(drawsAnything("[<b></b>](https://example.com)"))
    }

    @Test
    fun `empty emphasis is not drawn`() {
        assertFalse(drawsAnything("*<b></b>*"))
    }
}
