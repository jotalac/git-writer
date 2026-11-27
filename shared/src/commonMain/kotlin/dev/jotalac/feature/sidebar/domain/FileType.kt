package dev.jotalac.feature.sidebar.domain

enum class FileType {
    NOTE,
    GENERAL_FILE,
    DIRECTORY,
}

/** A markdown file is a note, anything else (an image, a pdf) is a general file. */
val FileNode.fileType: FileType
    get() = when (this) {
        is FileNode.Directory -> FileType.DIRECTORY
        is FileNode.File -> if (name.endsWith(".md", ignoreCase = true)) FileType.NOTE else FileType.GENERAL_FILE
    }
