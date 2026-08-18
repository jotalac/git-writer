package dev.jotalac.feature.git_sync.domain

import kotlinx.coroutines.flow.Flow

interface GitSyncRepository {
    suspend fun validateCredentials(repoUrl: String, tokenOrPassword: String, username: String? = null): Result<Boolean>
    suspend fun cloneRepository(
        repoUrl: String,
        tokenOrPassword: String,
        destinationPath: String,
        username: String? = null
    ): Result<Unit>

    suspend fun syncNotes(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String? = null,
        commitMessage: String = "sync notes"
    ): Result<SyncStatus>

    val syncStatus: Flow<SyncStatus>
}

sealed interface SyncStatus {
    data object UpToDate : SyncStatus
    data object PulledChanges : SyncStatus
    data object PushedChanges : SyncStatus
    data object PulledAndPushedChanges : SyncStatus
    data class Conflict(val files: List<String>) : SyncStatus
}