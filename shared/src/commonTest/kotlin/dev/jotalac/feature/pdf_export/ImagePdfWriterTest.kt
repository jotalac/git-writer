package dev.jotalac.feature.pdf_export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImagePdfWriterTest {

    private val pages = listOf(
        JpegPage("first page".encodeToByteArray(), widthPx = 1588, heightPx = 2246),
        JpegPage("second page".encodeToByteArray(), widthPx = 1588, heightPx = 2246),
        JpegPage("third".encodeToByteArray(), widthPx = 800, heightPx = 600),
    )

    // one character per byte, so positions in the text are byte offsets in the file
    private val pdf = writeImagePdf(pages).let { bytes -> CharArray(bytes.size) { (bytes[it].toInt() and 0xFF).toChar() }.concatToString() }

    @Test
    fun `the file is a PDF with one page per image`() {
        assertTrue(pdf.startsWith("%PDF-1.4\n"))
        assertTrue(pdf.endsWith("%%EOF\n"))
        assertEquals(3, Regex("""/Type /Page /""").findAll(pdf).count())
        assertTrue("/Count 3" in pdf)
    }

    @Test
    fun `every object is where the cross reference table says`() {
        // a viewer jumps to the objects by these offsets - one wrong byte and the file does not open
        val xrefOffset = pdf.substringAfterLast("startxref\n").substringBefore("\n").toInt()
        assertTrue(pdf.startsWith("xref\n", xrefOffset))

        val entries = pdf.substring(xrefOffset).lines().drop(3).takeWhile { it.endsWith(" n ") }
        assertEquals(2 + pages.size * 3, entries.size)
        entries.forEachIndexed { index, entry ->
            val objectNumber = index + 1
            val offset = entry.substringBefore(" ").toInt()
            assertTrue(pdf.startsWith("$objectNumber 0 obj\n", offset), "object $objectNumber is not at $offset")
        }
    }

    @Test
    fun `each image is embedded unchanged with its size`() {
        assertTrue("/Width 800 /Height 600" in pdf)
        assertTrue("/Filter /DCTDecode /Length 5 >>\nstream\nthird\nendstream" in pdf)
    }

    @Test
    fun `the image fills the whole A4 page`() {
        assertTrue("/MediaBox [0 0 595.28 841.89]" in pdf)
        assertTrue("q 595.28 0 0 841.89 0 0 cm /Im0 Do Q" in pdf)
    }
}
