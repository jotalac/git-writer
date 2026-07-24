package dev.jotalac.feature.editor_sidebar.domain

data class FlatFileNode(
    val node: FileNode,
    val depth: Int,
    val isExpanded: Boolean = false
)