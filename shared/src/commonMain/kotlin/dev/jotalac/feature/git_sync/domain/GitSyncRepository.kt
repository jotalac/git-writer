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

    suspend fun resolveSingleConflict(
        currentNotebookPath: String,
        conflictedFilePath: String,
        keepLocalChanges: Boolean,
    ): Result<Unit>

    suspend fun resolveAllConflicts(
        currentNotebookPath: String,
        keepLocalChanges: Boolean,
    ): Result<Unit>

    suspend fun pushChanges(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String? = null
    ): Result<Unit>

    val syncStatus: Flow<SyncStatus>
}

sealed interface SyncStatus {
    data object UpToDate : SyncStatus
    data class Conflict(val files: Set<String>) : SyncStatus
//    data object PulledChanges : SyncStatus
//    data object PushedChanges : SyncStatus
//    data object PulledAndPushedChanges : SyncStatus
}