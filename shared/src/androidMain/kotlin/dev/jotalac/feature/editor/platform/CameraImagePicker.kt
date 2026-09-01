package dev.jotalac.feature.editor.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogException
import io.github.vinceglb.filekit.dialogs.openCameraPicker
import io.github.vinceglb.filekit.readBytes

actual suspend fun pickCameraImage(): ByteArray? =
    try {
        FileKit.openCameraPicker()?.readBytes()
    } catch (_: FileKitDialogException) {
        null
    }
