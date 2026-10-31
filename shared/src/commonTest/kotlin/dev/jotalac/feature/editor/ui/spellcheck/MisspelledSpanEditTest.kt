package dev.jotalac.feature.editor.ui.spellcheck

import kotlin.test.Test
import kotlin.test.assertEquals

class MisspelledSpanEditTest {

    // "helo" at 0, "wrold" at 10
    private val text = "helo dear wrold"
    private val spans = listOf(MisspelledSpan(0, 4), MisspelledSpan(10, 15))

    @Test
    fun `spans after an edit move with the text`() {
        assertEquals(
            listOf(MisspelledSpan(0, 4), MisspelledSpan(13, 18)),
            spans.movedByEdit(text, "helo my dear wrold"),
        )
    }

    @Test
    fun `a span the edit touches is dropped - its word changed`() {
        // typing at the end of "helo"
        assertEquals(listOf(MisspelledSpan(11, 16)), spans.movedByEdit(text, "helos dear wrold"))
        // deleting into "wrold"
        assertEquals(listOf(MisspelledSpan(0, 4)), spans.movedByEdit(text, "helo dear rold"))
    }

    @Test
    fun `the same text keeps every span`() {
        assertEquals(spans, spans.movedByEdit(text, text))
    }
}
