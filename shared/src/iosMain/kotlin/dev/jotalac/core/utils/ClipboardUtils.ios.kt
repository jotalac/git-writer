package dev.jotalac.core.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
actual fun buildClipEntry(text: String): ClipEntry {
    return ClipEntry.withPlainText(text)
}

actual fun hasClipboardImage(): Boolean {
    return false
}

actual fun getImageBytesFromClipboard(): ByteArray? {
    return null
}