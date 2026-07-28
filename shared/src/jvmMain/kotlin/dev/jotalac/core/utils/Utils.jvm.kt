package dev.jotalac.core.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual fun buildClipEntry(text: String): ClipEntry {
    return ClipEntry(StringSelection(text))
}

actual val isDesktopPlatform = true