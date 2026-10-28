package dev.jotalac.core.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun ImageBitmap.encodeToJpeg(quality: Int): ByteArray {
    // a recorded layer comes back as a hardware bitmap, which cannot be compressed
    val bitmap = asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    try {
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}
