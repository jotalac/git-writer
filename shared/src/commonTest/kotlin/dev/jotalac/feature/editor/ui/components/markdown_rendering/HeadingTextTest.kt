package dev.jotalac.feature.editor.ui.components.markdown_rendering

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeadingTextTest {

    /** Whether the heading in [blockText], parsed like the editor parses it, has any text to draw. */
    private fun hasText(blockText: String): Boolean {
        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(blockText)
        val heading = tree.children.firstOrNull { it.type == MarkdownElementTypes.ATX_2 }
            ?: error("no heading in: $blockText")
        return heading.hasHeadingText(blockText)
    }

    @Test
    fun `a heading with a title has text`() {
        assertTrue(hasText("## Title"))
    }

    @Test
    fun `only hashes have no text`() {
        assertFalse(hasText("##"))
    }

    @Test
    fun `hashes followed by spaces have no text`() {
        assertFalse(hasText("##   "))
    }

    @Test
    fun `closing hashes alone have no text`() {
        assertFalse(hasText("## ##"))
    }
}
