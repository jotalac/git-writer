package dev.jotalac.core.utils

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

// do something like `rm -rf` on the specified path
fun Path.deleteRecursively() {
    val metadata = SystemFileSystem.metadataOrNull(this) ?: return

    if (metadata.isDirectory) {
        SystemFileSystem.list(this).forEach { childPath ->
            childPath.deleteRecursively()
        }
    }

    SystemFileSystem.delete(this, mustExist = false)
}