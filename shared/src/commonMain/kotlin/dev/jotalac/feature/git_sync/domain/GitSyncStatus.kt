package dev.jotalac.feature.git_sync.domain

sealed interface GitSyncStatus {
    data object UpToDate : GitSyncStatus
    data object Syncing : GitSyncStatus
    data object GitSyncFailed : GitSyncStatus
    data object GitSyncNotConfigured : GitSyncStatus
    data class Conflict(val files: Set<String>) : GitSyncStatus

    // shown instead of the git status while the remote cannot be reached, see EditorScreenState.syncButtonStatus
    data object Offline : GitSyncStatus
    data object RemoteUnreachable : GitSyncStatus
}