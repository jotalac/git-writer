package dev.jotalac.feature.pdf_export

import io.github.vinceglb.filekit.PlatformFile

// apps have no Downloads folder of their own - the document picker opens at its default location
actual fun defaultExportDirectory(): PlatformFile? = null
