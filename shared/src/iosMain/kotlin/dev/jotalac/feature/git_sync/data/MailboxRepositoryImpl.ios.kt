package dev.jotalac.feature.git_sync.data

import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.feature.git_sync.data.libgit2.GitCredentials
import dev.jotalac.feature.git_sync.data.libgit2.GitException
import dev.jotalac.feature.git_sync.data.libgit2.GitRepository
import dev.jotalac.feature.git_sync.data.libgit2.addRemote
import dev.jotalac.feature.git_sync.data.libgit2.commitAll
import dev.jotalac.feature.git_sync.data.libgit2.fetch
import dev.jotalac.feature.git_sync.data.libgit2.hasRemoteBranches
import dev.jotalac.feature.git_sync.data.libgit2.push
import dev.jotalac.feature.git_sync.data.libgit2.resetHard
import dev.jotalac.feature.git_sync.domain.MailboxRepository
import dev.jotalac.feature.git_sync.domain.PushRejectedException
import git2.GIT_ENONFASTFORWARD
import git2.git_libgit2_init
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

private const val ORIGIN = "origin"
private const val REMOTE_MAIN = "refs/remotes/origin/main"

// one operation at a time, like the sync - libgit2 must not work on one repository from two threads
internal val EncryptedSyncDispatcher = Dispatchers.IO.limitedParallelism(1)

/** iOS [MailboxRepository] on libgit2 - does what JGitMailboxRepositoryImpl does. */
@OptIn(ExperimentalForeignApi::class)
class LibGit2MailboxRepositoryImpl : MailboxRepository {

    init {
        git_libgit2_init()
    }

    override suspend fun create(path: String, remoteUrl: String): Result<Unit> = runGit {
        GitRepository.init(path, initialBranch = "main").use { it.addRemote(ORIGIN, remoteUrl) }
    }

    override suspend fun isEmpty(path: String): Result<Boolean> = runGit {
        GitRepository.open(path).use { !it.hasRemoteBranches(ORIGIN) }
    }

    override suspend fun update(path: String, tokenOrPassword: String, username: String?): Result<Unit> = runGit {
        GitRepository.open(path).use { repo ->
            repo.fetch(ORIGIN, GitCredentials(username, tokenOrPassword))
            // an empty mailbox has no main yet - nothing to take over
            if (repo.hasReference(REMOTE_MAIN)) repo.resetHard(REMOTE_MAIN)
        }
    }

    override suspend fun publish(
        path: String,
        message: String,
        tokenOrPassword: String,
        username: String?,
    ): Result<Unit> = runGit {
        GitRepository.open(path).use { repo ->
            repo.commitAll(message)
            try {
                repo.push(ORIGIN, GitCredentials(username, tokenOrPassword))
            } catch (e: GitException) {
                if (e.code == GIT_ENONFASTFORWARD) throw PushRejectedException(e.message.orEmpty())
                throw e
            }
        }
    }

    private suspend fun <T> runGit(block: () -> T): Result<T> = withContext(EncryptedSyncDispatcher) {
        suspendRunCatching(block)
    }
}
