package dev.jotalac.feature.editor.ui.utils

import dev.jotalac.testing.fieldOf
import dev.jotalac.testing.notation
import kotlin.test.Test
import kotlin.test.assertEquals

class TextFieldExtensionsTest {

    // --- inline syntax (bold, italic, code...)

    @Test
    fun `bold without a selection puts the cursor between the markers`() {
        assertEquals("make **|**", fieldOf("make |").applyBold().notation())
    }

    @Test
    fun `bold wraps the selection and keeps it selected`() {
        assertEquals("**«hello»** world", fieldOf("«hello» world").applyBold().notation())
    }

    @Test
    fun `a backwards selection is wrapped the same way`() {
        // selecting with shift + left arrow (or dragging backwards) gives a reversed selection
        assertEquals("**«hello»** world", fieldOf("«hello» world", reversed = true).applyBold().notation())
    }

    @Test
    fun `pressing bold twice gives the original text back`() {
        assertEquals("«hello» world", fieldOf("«hello» world").applyBold().applyBold().notation())
    }

    @Test
    fun `bold is removed when the markers are selected with the text`() {
        assertEquals("«hello» world", fieldOf("«**hello**» world").applyBold().notation())
    }

    @Test
    fun `empty markers are removed again when the cursor is between them`() {
        assertEquals("make |", fieldOf("make **|**").applyBold().notation())
    }

    @Test
    fun `italic on bold text makes it bold and italic instead of turning it into italic`() {
        assertEquals("***«both»***", fieldOf("**«both»**").applyItalic().notation())
    }

    @Test
    fun `removing italic from bold and italic text keeps the bold`() {
        assertEquals("**«both»**", fieldOf("***«both»***").applyItalic().notation())
    }

    @Test
    fun `removing bold from bold and italic text keeps the italic`() {
        assertEquals("*«both»*", fieldOf("***«both»***").applyBold().notation())
    }

    @Test
    fun `only the markers selected are wrapped without breaking`() {
        // the selection is shorter than both markers together - it used to throw when cutting them off
        assertEquals("**«**»**", fieldOf("«**»").applyBold().notation())
    }

    @Test
    fun `code block puts the selected code on its own lines`() {
        assertEquals("```\n«val x = 1»\n```", fieldOf("«val x = 1»").applyCodeBlock().notation())
    }

    @Test
    fun `link without a selection puts the cursor where the link text goes`() {
        assertEquals("see [|]()", fieldOf("see |").addLink().notation())
    }

    // --- line syntax (headings, lists, quote)

    @Test
    fun `a heading goes to the start of the line and not to the cursor`() {
        assertEquals("# Ti|tle", fieldOf("Ti|tle").applyH1().notation())
    }

    @Test
    fun `the same heading again removes it`() {
        assertEquals("Ti|tle", fieldOf("# Ti|tle").applyH1().notation())
    }

    @Test
    fun `another heading level is added in front instead of replacing the existing one`() {
        assertEquals("## # Ti|tle", fieldOf("# Ti|tle").applyH2().notation())
    }

    @Test
    fun `a cursor inside the removed prefix moves to where the text starts`() {
        assertEquals("|Title", fieldOf("#| Title").applyH1().notation())
    }

    @Test
    fun `the selection stays on the same text`() {
        assertEquals("> «quoted»", fieldOf("«quoted»").applyQuote().notation())
    }

    @Test
    fun `only the line with the cursor changes in a block of several lines`() {
        assertEquals("- one\ntw|o", fieldOf("- one\n- tw|o").applyBulletedList().notation())
    }

    @Test
    fun `a nested list item keeps its indentation`() {
        assertEquals("  - ite|m", fieldOf("  ite|m").applyBulletedList().notation())
        assertEquals("  ite|m", fieldOf("  - ite|m").applyBulletedList().notation())
    }

    @Test
    fun `the checkbox button inserts a valid task list item`() {
        // markdown only renders a checkbox with the space after "- [ ]", list continuation needs it too
        assertEquals("- [ ] |", fieldOf("|").applyCheckbox().notation())
    }
}
