package dev.jotalac.feature.git_sync.data

import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CheckoutCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class JGitSyncRepositoryImpl : GitSyncRepository {

    private fun getCredentials(username: String?, tokenOrPassword: String): UsernamePasswordCredentialsProvider {
        val safeName = if (username.isNullOrBlank()) tokenOrPassword else username
        return UsernamePasswordCredentialsProvider(safeName, tokenOrPassword)
    }

    override suspend fun validateCredentials(
        repoUrl: String,
        tokenOrPassword: String,
        username: String?
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val credentials = getCredentials(username, tokenOrPassword)

                //ls-remote to check accessibility to the remote without cloning any files
                val remoteRefs = Git.lsRemoteRepository()
                    .setRemote(repoUrl)
                    .setCredentialsProvider(credentials)
                    .call()

                remoteRefs.isNotEmpty()
            }
        }

    override suspend fun cloneRepository(
        repoUrl: String,
        tokenOrPassword: String,
        destinationPath: String,
        username: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // validate the credentials before cloning
        val credentialsValidationResult = validateCredentials(repoUrl, tokenOrPassword, username)
        if (credentialsValidationResult.isFailure) return@withContext Result.failure(
            credentialsValidationResult.exceptionOrNull() ?: Exception("Invalid repository credentials")
        )

        val destinationDirectory = File(destinationPath)

        // clone the repository
        suspendRunCatching {
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(destinationDirectory)
                .setCredentialsProvider(getCredentials(username, tokenOrPassword))
                .call()
                .use { }

        }
    }

    private fun stageAndCommitAll(git: Git, commitMessage: String) {
        val status = git.status().call()
        val hasLocalChanges = status.hasUncommittedChanges() || status.untracked.isNotEmpty()

        if (hasLocalChanges) {
            git.add().addFilepattern(".").call()
            git.add().addFilepattern(".").setUpdate(true).call() // capture deleted files
            git.commit().setMessage(commitMessage).call()
        }
    }

    private fun getLocalRepoDir(currentNotebookPath: String): File {
        val localRepoDir = File(currentNotebookPath)
        if (!localRepoDir.resolve(".git").exists()) {
            throw IllegalStateException("Notebook directory is not a Git repository.")
        }

        return File(currentNotebookPath)
    }


    override suspend fun syncNotes(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String?,
        commitMessage: String
    ): Result<GitSyncStatus> = withContext(Dispatchers.IO) {
        _syncStatus.tryEmit(GitSyncStatus.Syncing)
        suspendRunCatching {

            //check if the notebook directory is a git repo
            val localRepoDir = getLocalRepoDir(currentNotebookPath)

            Git.open(localRepoDir).use { git ->
                val credentials = getCredentials(username, tokenOrPassword)

                // stage and commit local changes
                stageAndCommitAll(git, commitMessage)

                // pull changes from remote
                val pullResult = git.pull().setCredentialsProvider(credentials).call()

                // resolve merge conflicts
                val mergeResult = pullResult.mergeResult
                if (mergeResult != null && !mergeResult.mergeStatus.isSuccessful) {
                    // return the conflicted files if the failure was due to the file conflict
                    if (mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING) {
                        val conflictingFiles = mergeResult.conflicts?.keys ?: emptySet()
                        val conflictStatus = GitSyncStatus.Conflict(conflictingFiles)
                        _syncStatus.tryEmit(conflictStatus)
                        return@suspendRunCatching conflictStatus
                    } else {
                        // Throw an error if the merge failed for a different reason
                        error("Merge failed critically: ${mergeResult.mergeStatus}")
                    }
                }

                //push everything
                git.push().setCredentialsProvider(credentials).call()

                val status = GitSyncStatus.UpToDate
                _syncStatus.tryEmit(status)
                status
            }

        }
    }

    override suspend fun resolveSingleConflict(
        currentNotebookPath: String,
        conflictedFilePath: String,
        keepLocalChanges: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val localRepoDir = getLocalRepoDir(currentNotebookPath)

            Git.open(localRepoDir).use { git ->
                // keep OURS or THEIRS version
                val stage = if (keepLocalChanges) CheckoutCommand.Stage.OURS else CheckoutCommand.Stage.THEIRS

                // checkout the requested variant
                git.checkout()
                    .setStage(stage)
                    .addPath(conflictedFilePath)
                    .call()

                // stage the file
                git.add().addFilepattern(conflictedFilePath).call()

                // check if there are any other conflicts - if not do the commit
                val status = git.status().call()

                if (status.conflicting.isEmpty()) {
                    git.commit().setMessage("Resolved conflict").call()
                }

                _syncStatus.tryEmit(GitSyncStatus.UpToDate)
                Unit
            }
        }
    }

    override suspend fun resolveAllConflicts(currentNotebookPath: String, keepLocalChanges: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val localRepoDir = getLocalRepoDir(currentNotebookPath)

                Git.open(localRepoDir).use { git ->
                    // get all the files that are conflicting
                    val status = git.status().call()
                    val conflictingFiles = status.conflicting.ifEmpty { return@use }

                    // set the state for all files
                    val stage = if (keepLocalChanges) CheckoutCommand.Stage.OURS else CheckoutCommand.Stage.THEIRS
                    val checkoutCommand = git.checkout().setStage(stage)

                    // checkout all conflicting files
                    for (file in conflictingFiles) {
                        checkoutCommand.addPath(file)
                    }
                    checkoutCommand.call()

                    stageAndCommitAll(git, "Resolved all conflicts - keep ${stage.name.lowercase()}")
                    _syncStatus.tryEmit(GitSyncStatus.UpToDate)
                    Unit
                }
            }
        }

    override suspend fun pushChanges(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val localRepoDir = getLocalRepoDir(currentNotebookPath)
            val credentials = getCredentials(username, tokenOrPassword)

            Git.open(localRepoDir).use { git ->
                git.push().setCredentialsProvider(credentials).call()
            }

            _syncStatus.tryEmit(GitSyncStatus.UpToDate)
            Unit
        }
    }

    private val _syncStatus = MutableSharedFlow<GitSyncStatus>(extraBufferCapacity = 1)
    override val gitSyncStatus: Flow<GitSyncStatus> = _syncStatus.asSharedFlow()

}