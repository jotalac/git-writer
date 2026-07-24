package dev.jotalac.core.utils

import dev.jotalac.feature.editor_sidebar.domain.FileNode
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

val ALLOWED_FILE_EXTENSIONS = listOf(".md", ".png", ".jpg", ".svg")