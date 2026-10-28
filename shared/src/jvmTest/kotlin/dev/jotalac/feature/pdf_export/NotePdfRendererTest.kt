package dev.jotalac.feature.pdf_export

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import coil3.SingletonImageLoader
import dev.jotalac.core.utils.getAsyncImageLoader
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/*
* Renders a real note the way the export does - the app's markdown components, Coil loading a local image,
* the page capture - offscreen in a Compose scene, and checks the pages that come out.
* */
class NotePdfRendererTest {

    private val folder = Files.createTempDirectory("pdf-export-test").toFile()

    @AfterTest
    fun cleanUp() {
        folder.deleteRecursively()
    }

    @Test
    fun `a long note with an image becomes several A4 pages with its content on them`() {
        SingletonImageLoader.setUnsafe { getAsyncImageLoader(it) }
        val image = redSquare(File(folder, "red.png"))
        val note = buildString {
            append("# Trip notes\n\n")
            append("```kotlin\nfun main() {\n    println(\"hello\")\n}\n```\n\n")
            append("![square|400](${image.absolutePath})\n\n")
            repeat(60) { append("Paragraph $it with some text so the note needs more than one page.\n\n") }
        }

        val pages = render(note).getOrThrow()

        assertTrue(pages.size >= 2, "a long note should span several pages, got ${pages.size}")
        pages.forEach {
            assertEquals(1588, it.widthPx)
            assertEquals(2246, it.heightPx)
        }
        val firstPage = decode(pages.first())
        assertTrue(firstPage.hasPixel { r, g, b -> r > 200 && g < 60 && b < 60 }, "the red image is missing on page 1")
        assertTrue(firstPage.hasPixel { r, g, b -> r < 90 && g < 90 && b < 90 }, "page 1 has no dark text")

        // for looking at the result by hand: ./shared/build/pdf-export-test/
        val output = File("build/pdf-export-test").apply { mkdirs() }
        pages.forEachIndexed { index, page -> File(output, "page-$index.jpg").writeBytes(page.bytes) }
        File(output, "note.pdf").writeBytes(writeImagePdf(pages))
    }

    private fun render(note: String): Result<List<JpegPage>> {
        var result: Result<List<JpegPage>>? = null
        val scene = ImageComposeScene(width = 300, height = 300, density = Density(1f)) {
            NotePdfRenderer(note) { result = it }
        }
        try {
            val deadline = TimeSource.Monotonic.markNow() + 60.seconds
            var frameTime = 0L
            while (result == null && deadline.hasNotPassedNow()) {
                scene.render(frameTime)
                frameTime += 16_000_000
                Thread.sleep(16)
            }
        } finally {
            scene.close()
        }
        return result ?: error("the pages were not rendered in time")
    }

    private fun redSquare(file: File): File {
        val surface = Surface.makeRasterN32Premul(200, 200)
        surface.canvas.drawRect(Rect.makeWH(200f, 200f), Paint().apply { color = Color.RED })
        file.writeBytes(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes)
        return file
    }

    private fun decode(page: JpegPage): Bitmap = Bitmap.makeFromImage(Image.makeFromEncoded(page.bytes))

    private fun Bitmap.hasPixel(matches: (r: Int, g: Int, b: Int) -> Boolean): Boolean {
        for (y in 0 until height step 2) for (x in 0 until width step 2) {
            val color = getColor(x, y)
            if (matches(Color.getR(color), Color.getG(color), Color.getB(color))) return true
        }
        return false
    }
}
