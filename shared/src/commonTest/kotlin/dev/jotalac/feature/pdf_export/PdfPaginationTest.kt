package dev.jotalac.feature.pdf_export

import kotlin.test.Test
import kotlin.test.assertEquals

class PdfPaginationTest {

    @Test
    fun `blocks that fit share a page`() {
        assertEquals(listOf(PageSlice(0, 600)), paginate(listOf(100, 200, 300), pageHeight = 1000))
    }

    @Test
    fun `a block exactly as tall as the space left still fits`() {
        assertEquals(listOf(PageSlice(0, 1000)), paginate(listOf(500, 500), pageHeight = 1000))
    }

    @Test
    fun `a block that no longer fits starts the next page instead of being cut`() {
        assertEquals(
            listOf(PageSlice(0, 800), PageSlice(800, 1200)),
            paginate(listOf(400, 400, 400), pageHeight = 1000),
        )
    }

    @Test
    fun `a block taller than a page fills the page and is cut at its edge`() {
        // a long code block - it would be cut anyway, so it does not leave the first page empty
        assertEquals(
            listOf(PageSlice(0, 1000), PageSlice(1000, 2000), PageSlice(2000, 2700)),
            paginate(listOf(100, 2500, 100), pageHeight = 1000),
        )
    }

    @Test
    fun `an empty note gives one empty page`() {
        assertEquals(listOf(PageSlice(0, 0)), paginate(emptyList(), pageHeight = 1000))
    }
}
