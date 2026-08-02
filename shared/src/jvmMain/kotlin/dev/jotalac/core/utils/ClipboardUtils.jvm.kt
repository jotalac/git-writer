package dev.jotalac.core.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalComposeUiApi::class)
actual fun buildClipEntry(text: String): ClipEntry {
    return ClipEntry(StringSelection(text))
}

actual fun hasClipboardImage(): Boolean {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        //copied files
        if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            return true
        }
        // copied file from file browser
        if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
            val files = clipboard.getData(DataFlavor.javaFileListFlavor) as? List<*>
            return files?.filterIsInstance<File>()?.any { isImageFile(it.name) } == true
        }
        false
    } catch (e: Exception) {
        false
    }
}

actual fun getImageBytesFromClipboard(): ByteArray? {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard

        if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            val rawImage = clipboard.getData(DataFlavor.imageFlavor) as? java.awt.Image ?: return null
            val bufferedImage =
                if (rawImage is BufferedImage && (rawImage.type == BufferedImage.TYPE_INT_ARGB || rawImage.type == BufferedImage.TYPE_INT_RGB)) {
                    rawImage
                } else {
                    val width = rawImage.getWidth(null)
                    val height = rawImage.getHeight(null)
                    if (width <= 0 || height <= 0) return null
                    val copy = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                    val graphics = copy.createGraphics()
                    graphics.drawImage(rawImage, 0, 0, null)
                    graphics.dispose()
                    copy
                }
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "png", outputStream)
            outputStream.toByteArray()
        } else if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
            val files = clipboard.getData(DataFlavor.javaFileListFlavor) as? List<*>
            val imageFile = files?.filterIsInstance<File>()?.firstOrNull { isImageFile(it.name) }
            imageFile?.readBytes()
        } else {
            null
        }
    } catch (e: Exception) {
        println("Failed to read image from clipboard: ${e.message}")
        null
    }
}