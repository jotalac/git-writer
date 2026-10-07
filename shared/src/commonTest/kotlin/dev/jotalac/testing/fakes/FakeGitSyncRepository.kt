package dev.jotalac.testing.fakes

import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeGitSyncRepository : GitSyncRepository {

    val status = MutableSharedFlow<GitSyncStatus>(replay = 1)
    override val gitSyncStatus: Flow<GitSyncStatus> = status

    var syncResult: Result<GitSyncStatus> = Result.failure(IllegalStateException("not stubbed"))

    val initializedPaths = mutableListOf<String>()
    val clonedDestinations = mutableListOf<String>()
    val syncedPaths = mutableListOf<String>()
    val pushedPaths = mutableListOf<String>()
    val updatedRemoteUrls = mutableListOf<Pair<String, String>>()
    val abortedMerges = mutableListOf<String>()
    val removedRemotes = mutableListOf<String>()

    override suspend fun validateCredentials(
        repoUrl: String,
        tokenOrPassword: String,
        username: String?,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun cloneRepository(
        repoUrl: String,
        tokenOrPassword: String,
        destinationPath: String,
        username: String?,
    ): Result<Unit> {
        clonedDestinations += destinationPath
        return Result.success(Unit)
    }

    override suspend fun initRepository(currentNotebookPath: String): Result<Unit> {
        initializedPaths += currentNotebookPath
        return Result.success(Unit)
    }

    override suspend fun syncNotes(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String?,
        commitMessage: String,
    ): Result<GitSyncStatus> {
        syncedPaths += currentNotebookPath
        return syncResult
    }

    override suspend fun resolveSingleConflict(
        currentNotebookPath: String,
        conflictedFilePath: String,
        keepLocalChanges: Boolean,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun resolveAllConflicts(
        currentNotebookPath: String,
        keepLocalChanges: Boolean,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun pushChanges(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String?,
    ): Result<Unit> {
        pushedPaths += currentNotebookPath
        return Result.success(Unit)
    }

    override suspend fun updateRemoteUrl(
        currentNotebookPath: String,
        newRemoteUrl: String,
    ): Result<Unit> {
        updatedRemoteUrls += currentNotebookPath to newRemoteUrl
        return Result.success(Unit)
    }

    override suspend fun abortMerge(currentNotebookPath: String): Result<Unit> {
        abortedMerges += currentNotebookPath
        return Result.success(Unit)
    }

    override suspend fun removeRemote(currentNotebookPath: String): Result<Unit> {
        removedRemotes += currentNotebookPath
        return Result.success(Unit)
    }

    override fun updateSyncStatus(remoteUrl: String?, syncStatus: GitSyncStatus?): Result<Unit> {
        syncStatus?.let { status.tryEmit(it) }
        return Result.success(Unit)
    }
}
