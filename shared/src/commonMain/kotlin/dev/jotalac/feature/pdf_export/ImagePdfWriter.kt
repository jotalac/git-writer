package dev.jotalac.feature.pdf_export

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeString

/** A4 in PDF points (1/72 inch). */
const val A4_WIDTH_PT = 595.28
const val A4_HEIGHT_PT = 841.89

/** A page image, already encoded as JPEG. */
class JpegPage(val bytes: ByteArray, val widthPx: Int, val heightPx: Int)

/**
 * A PDF with one page per image, each image filling its page. Only what a viewer needs: the catalog, the page
 * tree, the pages with their image and the cross-reference table pointing at every object. The JPEGs are
 * embedded as they are (DCTDecode), so nothing has to be re-encoded.
 */
fun writeImagePdf(
    pages: List<JpegPage>,
    pageWidthPt: Double = A4_WIDTH_PT,
    pageHeightPt: Double = A4_HEIGHT_PT,
): ByteArray {
    val pdf = Buffer()
    // byte offset of every object, in object number order (1, 2, ...)
    val offsets = mutableListOf<Long>()

    fun writeObject(body: Buffer.() -> Unit) {
        offsets += pdf.size
        pdf.writeString("${offsets.size} 0 obj\n")
        pdf.body()
        pdf.writeString("\nendobj\n")
    }

    // objects: 1 catalog, 2 page tree, then 3 per page - the page, its content and its image
    fun pageObject(index: Int) = 3 + index * 3

    // the binary comment tells transfer tools the file is not plain text
    pdf.writeString("%PDF-1.4\n%")
    pdf.write(byteArrayOf(0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte()))
    pdf.writeString("\n")

    writeObject { writeString("<< /Type /Catalog /Pages 2 0 R >>") }
    writeObject {
        val kids = pages.indices.joinToString(" ") { "${pageObject(it)} 0 R" }
        writeString("<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>")
    }

    val width = pageWidthPt.toPdfNumber()
    val height = pageHeightPt.toPdfNumber()
    pages.forEachIndexed { index, page ->
        val pageNumber = pageObject(index)
        writeObject {
            writeString(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $width $height] " +
                    "/Resources << /XObject << /Im0 ${pageNumber + 2} 0 R >> >> /Contents ${pageNumber + 1} 0 R >>"
            )
        }
        writeObject {
            // the image is a 1x1 unit square - scaled to the page size
            val content = "q $width 0 0 $height 0 0 cm /Im0 Do Q"
            writeString("<< /Length ${content.length} >>\nstream\n$content\nendstream")
        }
        writeObject {
            writeString(
                "<< /Type /XObject /Subtype /Image /Width ${page.widthPx} /Height ${page.heightPx} " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${page.bytes.size} >>\nstream\n"
            )
            write(page.bytes)
            writeString("\nendstream")
        }
    }

    // every entry is exactly 20 bytes - the offset, the generation, in use, and a two byte line end
    val xrefOffset = pdf.size
    pdf.writeString("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
    offsets.forEach { pdf.writeString("${it.toString().padStart(10, '0')} 00000 n \n") }
    pdf.writeString("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

    return pdf.readByteArray()
}

// PDF numbers are plain decimals - "595.28", or "842" without a fraction
private fun Double.toPdfNumber(): String =
    if (this % 1.0 == 0.0) toLong().toString() else ((this * 100).toLong() / 100.0).toString()
