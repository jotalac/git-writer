package dev.jotalac.feature.editor.ui

import dev.jotalac.feature.editor.domain.EditorTabItem
import dev.jotalac.feature.git_sync.domain.GitSyncStatus

data class EditorScreenState(
    val isImage: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val gitSyncStatus: GitSyncStatus = GitSyncStatus.UpToDate,
    val conflictedFiles: List<String> = emptyList(),
    val openedTabs: List<EditorTabItem> = listOf(EditorTabItem(id = 0L, notePath = null)),
    val activeTabId: Long = 0L,
) {
    val activeTab: EditorTabItem?
        get() = openedTabs.firstOrNull { it.id == activeTabId }

    /** The note currently shown in the editor, derived from the active tab. */
    val activeNotePath: String?
        get() = activeTab?.notePath

    val activeFilename: String?
        get() = activeTab?.filename
}
