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

fun Path.buildFileTree() : FileNode.Directory {
    return FileNode.Directory(
        name = this.name,
        path = this.toString(),
        children = this.traverse()
    )
}

fun Path.traverse() : List<FileNode> {
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
    }.sortedWith(compareBy<FileNode> { it is FileNode.File}.thenBy { it.name })
}

val IMAGE_FILE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".svg")
val ALLOWED_FILE_EXTENSIONS = listOf(".md") + IMAGE_FILE_EXTENSIONS

fun isImageFile(filename: String): Boolean {
    val lowerName = filename.lowercase()
    return IMAGE_FILE_EXTENSIONS.any { lowerName.endsWith(it) }
}