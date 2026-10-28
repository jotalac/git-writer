package dev.jotalac.core.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun ImageBitmap.encodeToJpeg(quality: Int): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes
        ?: error("the image could not be encoded as JPEG")
