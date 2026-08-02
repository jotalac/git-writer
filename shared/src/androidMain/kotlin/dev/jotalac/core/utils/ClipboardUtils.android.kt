package dev.jotalac.core.utils

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

actual fun buildClipEntry(text: String): ClipEntry {
    return ClipData.newPlainText("Copied Path", text).toClipEntry()
}

actual fun hasClipboardImage(): Boolean {
    return false
}

actual fun getImageBytesFromClipboard(): ByteArray? {
    return null
}