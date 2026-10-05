package dev.jotalac.core.utils

actual val isDesktopPlatform = true

actual val isMacOsPlatform = System.getProperty("os.name").orEmpty().startsWith("Mac")

actual val isWindowsPlatform = System.getProperty("os.name").lowercase().startsWith("win")