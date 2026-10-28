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
    var resolveConflictResult: Result<Unit> = Result.success(Unit)
    var abortResult: Result<Unit> = Result.success(Unit)
    var pushResult: Result<Unit> = Result.success(Unit)
    var initResult: Result<Unit> = Result.success(Unit)
    var validateCredentialsResult: Result<Unit> = Result.success(Unit)
    var cloneResult: Result<Unit> = Result.success(Unit)
    /** what a clone puts into its destination - e.g. the files of an encrypted notebook's remote */
    var onClone: (destination: String) -> Unit = {}

    val initializedPaths = mutableListOf<String>()
    val clonedDestinations = mutableListOf<String>()
    val syncedPaths = mutableListOf<String>()
    val pushedPaths = mutableListOf<String>()
    val updatedRemoteUrls = mutableListOf<Pair<String, String>>()
    val abortedMerges = mutableListOf<String>()
    val removedRemotes = mutableListOf<String>()
    /** remote url of every updateSyncStatus call */
    val syncStatusRemoteUrls = mutableListOf<String?>()

    /** conflicted file to whether the local version was kept */
    val resolvedConflicts = mutableListOf<Pair<String, Boolean>>()
    /** keepLocalChanges of every resolve-all call */
    val resolvedAllConflicts = mutableListOf<Boolean>()

    override suspend fun validateCredentials(
        repoUrl: String,
        tokenOrPassword: String,
        username: String?,
    ): Result<Unit> = validateCredentialsResult

    override suspend fun cloneRepository(
        repoUrl: String,
        tokenOrPassword: String,
        destinationPath: String,
        username: String?,
    ): Result<Unit> {
        clonedDestinations += destinationPath
        onClone(destinationPath)
        return cloneResult
    }

    override suspend fun initRepository(currentNotebookPath: String): Result<Unit> {
        initializedPaths += currentNotebookPath
        return initResult
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
    ): Result<Unit> {
        resolvedConflicts += conflictedFilePath to keepLocalChanges
        return resolveConflictResult
    }

    override suspend fun resolveAllConflicts(
        currentNotebookPath: String,
        keepLocalChanges: Boolean,
    ): Result<Unit> {
        resolvedAllConflicts += keepLocalChanges
        return resolveConflictResult
    }

    override suspend fun pushChanges(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String?,
    ): Result<Unit> {
        pushedPaths += currentNotebookPath
        return pushResult
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
        return abortResult
    }

    override suspend fun removeRemote(currentNotebookPath: String): Result<Unit> {
        removedRemotes += currentNotebookPath
        return Result.success(Unit)
    }

    override fun updateSyncStatus(remoteUrl: String?, syncStatus: GitSyncStatus?): Result<Unit> {
        syncStatusRemoteUrls += remoteUrl
        syncStatus?.let { status.tryEmit(it) }
        return Result.success(Unit)
    }
}
