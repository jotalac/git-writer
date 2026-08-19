package dev.jotalac.feature.git_sync.domain

sealed interface GitSyncStatus {
    data object UpToDate : GitSyncStatus
    data object Syncing : GitSyncStatus
    data object GitSyncFailed : GitSyncStatus
    data object GitSyncNotConfigured : GitSyncStatus
    data class Conflict(val files: Set<String>) : GitSyncStatus
//    data object PulledChanges : SyncStatus
//    data object PushedChanges : SyncStatus
//    data object PulledAndPushedChanges : SyncStatus
}