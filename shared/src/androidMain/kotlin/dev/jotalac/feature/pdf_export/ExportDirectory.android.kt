package dev.jotalac.feature.pdf_export

import io.github.vinceglb.filekit.PlatformFile

// the system save dialog does not take a starting folder - it opens where the user saved last time
actual fun defaultExportDirectory(): PlatformFile? = null
