package dev.jotalac.core.utils

import androidx.compose.ui.platform.ClipEntry

expect fun buildClipEntry(text: String): ClipEntry

expect val isDesktopPlatform: Boolean