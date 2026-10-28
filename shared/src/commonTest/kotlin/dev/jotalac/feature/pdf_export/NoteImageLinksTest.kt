package dev.jotalac.feature.pdf_export

import kotlin.test.Test
import kotlin.test.assertEquals

class NoteImageLinksTest {

    @Test
    fun `every image of the note is found - also sized ones and links in angle brackets`() {
        val note = """
            # Trip
            ![beach](images/beach.png)
            text ![map|300x200](images/map.png) more text
            ![photo](<images/my photo.jpg>)
            ![](https://example.com/logo.svg)
        """.trimIndent()

        assertEquals(
            listOf("images/beach.png", "images/map.png", "images/my photo.jpg", "https://example.com/logo.svg"),
            imageLinks(note),
        )
    }

    @Test
    fun `links that are not images are left out and an image used twice is loaded once`() {
        val note = "[docs](https://example.com) ![a](a.png) and again ![a](a.png)"

        assertEquals(listOf("a.png"), imageLinks(note))
    }
}
