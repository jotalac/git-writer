package dev.jotalac.core.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
actual fun buildClipEntry(text: String): ClipEntry {
    return ClipEntry(StringSelection(text))
}

actual fun hasClipboardImage(): Boolean {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            return true
        }
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
            val rawImage = clipboard.getData(DataFlavor.imageFlavor) as? java.awt.Image
            rawImage?.toPngByteArray()
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