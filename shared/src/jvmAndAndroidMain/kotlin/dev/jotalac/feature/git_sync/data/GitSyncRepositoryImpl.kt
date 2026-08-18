package dev.jotalac.feature.git_sync.data

import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
            runCatching {
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
        runCatching {
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(destinationDirectory)
                .setCredentialsProvider(getCredentials(username, tokenOrPassword))
                .call()
                .use { }

        }
    }


    override suspend fun syncNotes(
        currentNotebookPath: String,
        tokenOrPassword: String,
        username: String?,
        commitMessage: String
    ): Result<SyncStatus> = withContext(Dispatchers.IO) {
        runCatching {
            //check if the notebook directory is a git repo
            val localRepoDir = File(currentNotebookPath)
            if (!localRepoDir.resolve(".git").exists()) {
                error("Notebook directory is not a Git repository.")
            }

            Git.open(localRepoDir).use { git ->
                val credentials = getCredentials(username, tokenOrPassword)

                // stage and commit local changes
                val status = git.status().call()
                val hasLocalChanges = status.hasUncommittedChanges() || status.untracked.isNotEmpty()

                if (hasLocalChanges) {
                    git.add().addFilepattern(".").call()
                    git.add().addFilepattern(".").setUpdate(true).call() // capture deleted files
                    git.commit().setMessage(commitMessage).call()
                }

                // pull changes from remote
                val pullResult = git.pull().setCredentialsProvider(credentials).call()

                // resolve merge conflicts
                val mergeResult = pullResult.mergeResult
                if (mergeResult != null && !mergeResult.mergeStatus.isSuccessful) {

                    // Check if the failure was specifically due to a file conflict
                    if (mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING) {

                        val conflictingFiles = mergeResult.conflicts?.keys ?: emptySet()

                        for (file in conflictingFiles) {
                            // 1. Force checkout the remote version (THEIRS)
                            git.checkout()
                                .setStage(CheckoutCommand.Stage.THEIRS)
                                .addPath(file)
                                .call()

                            // 2. Stage the newly checked-out file
                            git.add().addFilepattern(file).call()
                        }

                        // 3. Finalize the resolution with a merge commit
                        git.commit().setMessage("Auto-resolved conflicts preferring THEIRS (Testing)").call()

                    } else {
                        // Throw an error if the merge failed for a different reason (e.g., failed to write to disk)
                        error("Merge failed critically: ${mergeResult.mergeStatus}")
                    }
                }

                //push everything
                git.push().setCredentialsProvider(credentials).call()

                SyncStatus.UpToDate
            }

        }
    }

    override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.UpToDate)

}