package dev.jotalac.feature.git_sync.domain

import kotlinx.coroutines.flow.Flow

interface GitSyncRepository {
    suspend fun validateCredentials(repoUrl: String, tokenOrPassword: String, username: String? = null): Result<Unit>
    suspend fun cloneRepository(
        repoUrl: String,
        tokenOrPassword: String,
        destinationPath: String,
        username: String? = null
    ): Result<Unit>

    suspend fun initRepository(currentNotebookPath: String): Result<Unit>

    suspend fun syncNotes(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String? = null,
        commitMessage: String = "sync notes"
    ): Result<GitSyncStatus>

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

    suspend fun updateRemoteUrl(
        currentNotebookPath: String,
        newRemoteUrl: String,
    ): Result<Unit>

    suspend fun abortMerge(currentNotebookPath: String): Result<Unit>

    suspend fun removeRemote(currentNotebookPath: String): Result<Unit>

    fun updateSyncStatus(remoteUrl: String?, syncStatus: GitSyncStatus? = null): Result<Unit>

    val gitSyncStatus: Flow<GitSyncStatus>
}