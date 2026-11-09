package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PairedMarksTest {

    // "|" is the cursor, "[...]" a selection
    private fun value(marked: String): TextFieldValue {
        val open = marked.indexOf('[')
        if (open >= 0) {
            val close = marked.indexOf(']') - 1
            return TextFieldValue(marked.replace("[", "").replace("]", ""), TextRange(open, close))
        }
        val cursor = marked.indexOf('|')
        return TextFieldValue(marked.replace("|", ""), TextRange(cursor))
    }

    private fun marked(value: TextFieldValue?): String? {
        value ?: return null
        val s = value.selection
        if (s.collapsed) return value.text.substring(0, s.start) + "|" + value.text.substring(s.start)
        return value.text.substring(0, s.min) + "[" + value.text.substring(s.min, s.max) + "]" + value.text.substring(s.max)
    }

    private fun type(marked: String, char: Char) = marked(value(marked).typePairedMark(char))

    @Test
    fun `pairs at the start of the text or a line and after a space`() {
        assertEquals("`|`", type("|", '`'))
        assertEquals("a\n*|*", type("a\n|", '*'))
        assertEquals("a _|_ b", type("a | b", '_'))
    }

    @Test
    fun `in the middle of a word it is typed as it is`() {
        assertNull(type("snake|", '_'))
        assertNull(type("2|", '*'))
    }

    @Test
    fun `before a word it is typed as it is`() {
        assertNull(type("|word", '*'))
        assertNull(type(" |*word*", '*'))
    }

    @Test
    fun `steps over the closing mark`() {
        assertEquals("`code`|", type("`code|`", '`'))
        assertEquals("**bold*|*", type("**bold|**", '*'))
        assertEquals("**bold**|", type("**bold*|*", '*'))
    }

    @Test
    fun `doubles an empty pair`() {
        assertEquals("**|**", type("*|*", '*'))
        assertEquals("```|```", type("``|``", '`'))
    }

    @Test
    fun `wraps a selection`() {
        assertEquals("a *[word]* b", type("a [word] b", '*'))
        assertEquals("in`[side]`", type("in[side]", '`'))
    }

    @Test
    fun `a space in an empty single star pair starts a list item`() {
        assertEquals("* |", type("*|*", ' '))
        assertNull(type("**|**", ' '))
        assertNull(type("`|`", ' '))
        assertNull(type("a |", ' '))
    }

    @Test
    fun `backspace deletes an empty pair`() {
        assertEquals("a |", marked(value("a `|`").deleteEmptyPair()))
        assertEquals("*|*", marked(value("**|**").deleteEmptyPair()))
        assertNull(value("**bold*|*").deleteEmptyPair())
        assertNull(value("`a|`").deleteEmptyPair())
    }
}
