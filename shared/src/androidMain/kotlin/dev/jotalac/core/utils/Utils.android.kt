package dev.jotalac.core.utils

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

actual fun buildClipEntry(text: String): ClipEntry {
    return ClipData.newPlainText("Copied Path", text).toClipEntry()
}

actual val isDesktopPlatform = false