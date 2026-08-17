package dev.jotalac.core.utils

import dev.jotalac.feature.editor_sidebar.domain.FileNode
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

fun String.toSafeFileName(): String {
    val invalidCharacters = Regex("[\\\\/:*?\"<>|]")

    return this
        .replace(invalidCharacters, "_")
        .trim()
        .trimEnd('.')
}

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

fun Path.buildFileTree(): FileNode.Directory {
    return FileNode.Directory(
        name = this.name,
        path = this.toString(),
        children = this.traverse()
    )
}

fun Path.traverse(): List<FileNode> {
    val metadata = SystemFileSystem.metadataOrNull(this) ?: return emptyList()
    if (!metadata.isDirectory) return emptyList()

    return SystemFileSystem.list(this).mapNotNull { childPath ->
        val childMetadata = SystemFileSystem.metadataOrNull(childPath) ?: return@mapNotNull null
        val childName = childPath.name

        //ignore hidden files/folders
        if (childName.startsWith(".")) return@mapNotNull null

        if (childMetadata.isDirectory) {
            FileNode.Directory(
                name = childName,
                path = childPath.toString(),
                children = childPath.traverse()
            )
        } else {
            //check if the file is allowed file type
            if (ALLOWED_FILE_EXTENSIONS.any { childName.lowercase().endsWith(it) }) {
                FileNode.File(
                    name = childName,
                    path = childPath.toString(),
                )
            } else null
        }
    }.sortedWith(compareBy<FileNode> { it is FileNode.File }.thenBy { it.name })
}

val IMAGE_FILE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".svg", ".gif", ".webp")
val ALLOWED_FILE_EXTENSIONS = listOf(".md") + IMAGE_FILE_EXTENSIONS

fun isImageFile(filename: String): Boolean {
    val lowerName = filename.lowercase()
    return IMAGE_FILE_EXTENSIONS.any { lowerName.endsWith(it) }
}

// detect file type from the image bytes (ai generated)
fun ByteArray.detectImageExtension(): String {
    if (size >= 3 && this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte() && this[2] == 0xFF.toByte()) {
        return ".jpg"
    }
    if (size >= 3 && this[0] == 0x47.toByte() && this[1] == 0x49.toByte() && this[2] == 0x46.toByte()) {
        return ".gif"
    }
    if (size >= 12 && this[0] == 0x52.toByte() && this[1] == 0x49.toByte() && this[2] == 0x46.toByte() && this[3] == 0x46.toByte() &&
        this[8] == 0x57.toByte() && this[9] == 0x45.toByte() && this[10] == 0x42.toByte() && this[11] == 0x50.toByte()
    ) {
        return ".webp"
    }
    return ".png"
}