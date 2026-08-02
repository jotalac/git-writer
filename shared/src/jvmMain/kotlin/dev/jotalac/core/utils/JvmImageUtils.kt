package dev.jotalac.core.utils

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal fun java.awt.Image.toPngByteArray(): ByteArray? {
    return try {
        val bufferedImage = if (this is BufferedImage && (this.type == BufferedImage.TYPE_INT_ARGB || this.type == BufferedImage.TYPE_INT_RGB)) {
            this
        } else {
            val width = getWidth(null)
            val height = getHeight(null)
            if (width <= 0 || height <= 0) return null
            val copy = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = copy.createGraphics()
            graphics.drawImage(this, 0, 0, null)
            graphics.dispose()
            copy
        }
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        println("Failed to convert image to byte array: ${e.message}")
        null
    }
}
