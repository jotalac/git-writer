package dev.jotalac.feature.editor.ui.components.editor_find

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownFindStateTest {

    // "milk" at 11, 22 and 41
    private val text = "# Shopping\nmilk, more milk\nno match here\nMILK"

    // --- findAll

    @Test
    fun `finds every match in the note ignoring case`() {
        assertEquals(
            listOf(
                FindMatch(start = 11, end = 15),
                FindMatch(start = 22, end = 26),
                FindMatch(start = 41, end = 45),
            ),
            findAll(text, "milk"),
        )
    }

    @Test
    fun `case sensitive search skips other casing`() {
        assertEquals(listOf(FindMatch(41, 45)), findAll(text, "MILK", ignoreCase = false))
    }

    @Test
    fun `matches do not overlap`() {
        assertEquals(listOf(FindMatch(0, 2), FindMatch(2, 4)), findAll("aaaa", "aa"))
    }

    @Test
    fun `an empty query finds nothing`() {
        assertEquals(emptyList(), findAll(text, ""))
    }

    // --- find bar state

    @Test
    fun `typing a query jumps to the first match`() {
        val find = MarkdownFindState()

        find.updateQuery("milk", text)

        assertEquals(3, find.matches.size)
        assertEquals(0, find.currentIndex)
    }

    @Test
    fun `next goes through the matches and wraps to the first`() {
        val find = MarkdownFindState().apply { updateQuery("milk", text) }

        assertEquals(FindMatch(22, 26), find.findNext())
        assertEquals(FindMatch(41, 45), find.findNext())
        assertEquals(FindMatch(11, 15), find.findNext())
    }

    @Test
    fun `previous from the first match wraps to the last`() {
        val find = MarkdownFindState().apply { updateQuery("milk", text) }

        assertEquals(FindMatch(41, 45), find.findPrevious())
        assertEquals(2, find.currentIndex)
    }

    @Test
    fun `changing the query starts again from the first match`() {
        val find = MarkdownFindState().apply { updateQuery("milk", text) }
        find.findNext()

        find.updateQuery("milk,", text)

        assertEquals(listOf(FindMatch(11, 16)), find.matches)
        assertEquals(0, find.currentIndex)
    }

    @Test
    fun `next and previous do nothing without matches`() {
        val find = MarkdownFindState().apply { updateQuery("bread", text) }

        assertNull(find.findNext())
        assertNull(find.findPrevious())
    }

    @Test
    fun `closing the find bar clears the search`() {
        val find = MarkdownFindState().apply {
            open()
            updateQuery("milk", text)
            findNext()
        }

        find.close()

        assertFalse(find.isOpen)
        assertEquals("", find.query)
        assertTrue(find.matches.isEmpty())
        assertEquals(0, find.currentIndex)
    }
}
