package dev.jotalac.feature.editor.domain

data class EditorTabItem(
    val id: Long,
    val notePath: String?,
) {
    val filename: String?
        get() = notePath?.substringAfterLast('/')
}
