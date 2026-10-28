package dev.jotalac.feature.pdf_export

import io.github.vinceglb.filekit.PlatformFile

/** Where the save dialog of an export opens - the Downloads folder where there is one, null for the default. */
expect fun defaultExportDirectory(): PlatformFile?
