package dev.jotalac.core.utils

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

actual fun buildClipEntry(text: String): ClipEntry {
    val clipData = ClipData.newPlainText("Copied Text", text)
    return clipData.toClipEntry()
}

actual fun hasClipboardImage(): Boolean {
    return false
}

actual fun getImageBytesFromClipboard(): ByteArray? {
    return null
}