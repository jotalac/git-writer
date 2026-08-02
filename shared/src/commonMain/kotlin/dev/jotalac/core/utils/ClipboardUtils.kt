package dev.jotalac.core.utils

import androidx.compose.ui.platform.ClipEntry

expect fun buildClipEntry(text: String): ClipEntry

expect fun hasClipboardImage(): Boolean

expect fun getImageBytesFromClipboard(): ByteArray?