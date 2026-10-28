package dev.jotalac.feature.pdf_export

import io.github.vinceglb.filekit.PlatformFile
import java.io.File

// ~/Downloads on macOS, Windows and most Linux desktops - a translated folder name falls back to the default
actual fun defaultExportDirectory(): PlatformFile? =
    File(System.getProperty("user.home"), "Downloads").takeIf { it.isDirectory }?.let { PlatformFile(it) }
