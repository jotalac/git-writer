package dev.jotalac.feature.git_sync.data

import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
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
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
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


    override suspend fun syncNotes(commitMessage: String): Result<SyncStatus> {
        TODO("Not yet implemented")
    }

}