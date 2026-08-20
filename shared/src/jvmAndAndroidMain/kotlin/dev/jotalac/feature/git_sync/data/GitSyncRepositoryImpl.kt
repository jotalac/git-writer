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
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.transport.URIish
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
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val credentials = getCredentials(username, tokenOrPassword)

                //ls-remote to check accessibility to the remote without cloning any files
                Git.lsRemoteRepository()
                    .setRemote(repoUrl)
                    .setCredentialsProvider(credentials)
                    .call()

                Unit
            }
        }

    override suspend fun initRepository(currentNotebookPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val currentNotebookPath = File(currentNotebookPath)
            Git
                .init()
                .setDirectory(currentNotebookPath)
                .setInitialBranch("main")
                .call().use { }
        }
    }

    override suspend fun cloneRepository(
        repoUrl: String,
        tokenOrPassword: String,
        destinationPath: String,
        username: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            // validate the credentials before cloning
            validateCredentials(repoUrl, tokenOrPassword, username)

            val destinationDirectory = File(destinationPath)

            // clone the repository
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
                val pullResult = pullChanges(git, credentials)

                if (pullResult != null) {
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
                }

                //push everything
                pushChanges(git, credentials)

                val status = GitSyncStatus.UpToDate
                _syncStatus.tryEmit(status)
                status
            }

        }
    }

    private fun pullChanges(git: Git, credentials: UsernamePasswordCredentialsProvider): PullResult? {
        // handle the case where the repo is empty (no remote refs)
        return try {
            git.pull().setCredentialsProvider(credentials).call()
        } catch (e: Exception) {
            if (e.message?.contains("did not advertise Ref") == true) {
                null
            } else {
                throw e
            }
        }
    }

    private fun pushChanges(git: Git, credentials: UsernamePasswordCredentialsProvider) {
        val currentBranch = git.repository.branch
        val trackingStatus = BranchTrackingStatus.of(git.repository, currentBranch)

        val shouldPush = trackingStatus == null || trackingStatus.aheadCount > 0

        if (shouldPush) {
            git.push().setCredentialsProvider(credentials).call()
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

    override suspend fun updateRemoteUrl(currentNotebookPath: String, newRemoteUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val localRepoDir = getLocalRepoDir(currentNotebookPath)

                Git.open(localRepoDir).use { git ->
                    val allRemotes = git.remoteList().call()
                    val parsedUri = URIish(newRemoteUrl)

                    // if there is no remote, add it - else update the remote url
                    if (allRemotes.isEmpty()) {
                        git.remoteAdd().setName(defaultRemoteName).setUri(parsedUri).call()
                        setupBranchUpstream(git)
                    } else {
                        git.remoteSetUrl().setRemoteName(defaultRemoteName).setRemoteUri(parsedUri).call()
                    }
                }

                Unit
            }
        }

    private suspend fun setupBranchUpstream(git: Git) = withContext(Dispatchers.IO) {
        val currentBranch = git.repository.branch ?: "main"
        val config = git.repository.config

        config.setString("branch", currentBranch, "remote", defaultRemoteName)
        config.setString("branch", currentBranch, "merge", "refs/heads/$currentBranch")
        config.save()
    }

    private val defaultRemoteName = "origin"

    private val _syncStatus = MutableSharedFlow<GitSyncStatus>(extraBufferCapacity = 1)
    override val gitSyncStatus: Flow<GitSyncStatus> = _syncStatus.asSharedFlow()

}