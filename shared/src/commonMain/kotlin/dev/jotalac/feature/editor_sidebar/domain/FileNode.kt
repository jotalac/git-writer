package dev.jotalac.feature.editor_sidebar.domain


sealed class FileNode {
    abstract val name: String
    abstract val path: String

    data class File(
        override val name: String,
        override val path: String,
    ) : FileNode()

    data class Directory(
        override val name: String,
        override val path: String,
        val children: List<FileNode>,
    ) : FileNode()
}

