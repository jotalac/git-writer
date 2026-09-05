package dev.jotalac.feature.editor.domain

import kotlinx.io.files.Path

data class EditorTabItem(
    val id: Long,
    val notePath: String?,
) {
    val filename: String?
        get() = if (notePath != null) { Path(notePath).name } else null
}
