package dev.jotalac.feature.editor_sidebar.ui

sealed interface SidebarAction {
    data class OpenNote(val notePath: String) : SidebarAction
    data class AddNote(val path: String? = null) : SidebarAction
    data class AddFolder(val folderPath: String? = null) : SidebarAction
    data class MoveItem(val sourcePath: String, val destinationDirectoryPath: String) : SidebarAction
    data class DeleteItem(val path: String) : SidebarAction
    data class RenameItem(val newName: String, val path: String) : SidebarAction
    data class DuplicateNote(val notePath: String) : SidebarAction
    data class CopyItemPath(val path: String) : SidebarAction
    data class SetRenameItem(val path: String?) : SidebarAction
}