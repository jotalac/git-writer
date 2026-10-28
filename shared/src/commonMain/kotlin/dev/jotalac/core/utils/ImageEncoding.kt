package dev.jotalac.core.utils

import androidx.compose.ui.graphics.ImageBitmap

/** The bitmap as JPEG - without transparency, so draw it on an opaque background first. */
expect fun ImageBitmap.encodeToJpeg(quality: Int = 90): ByteArray
