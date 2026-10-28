package dev.jotalac.feature.git_sync.data

import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.feature.git_sync.domain.MailboxRepository
import dev.jotalac.feature.git_sync.domain.PushRejectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish
import java.io.File

private const val ORIGIN = "origin"
private const val REMOTE_MAIN = "refs/remotes/origin/main"

class JGitMailboxRepositoryImpl : MailboxRepository {

    override suspend fun create(path: String, remoteUrl: String): Result<Unit> = runGit {
        Git.init().setDirectory(File(path)).setInitialBranch("main").call().use { git ->
            git.remoteAdd().setName(ORIGIN).setUri(URIish(remoteUrl)).call()
        }
    }

    override suspend fun isEmpty(path: String): Result<Boolean> = runGit {
        Git.open(File(path)).use { it.repository.resolve(BUNDLE_MAIN_REF) == null }
    }

    override suspend fun update(path: String, tokenOrPassword: String, username: String?): Result<Unit> = runGit {
        Git.open(File(path)).use { git ->
            git.fetch().setRemote(ORIGIN).setCredentialsProvider(credentialsProvider(username, tokenOrPassword)).call()
            // an empty mailbox has no main yet - nothing to take over
            if (git.repository.resolve(REMOTE_MAIN) != null) {
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(REMOTE_MAIN).call()
            }
        }
    }

    override suspend fun publish(
        path: String,
        message: String,
        tokenOrPassword: String,
        username: String?,
    ): Result<Unit> = runGit {
        Git.open(File(path)).use { git ->
            git.add().addFilepattern(".").call()
            git.commit().setMessage(message).call()

            val updates = git.push()
                .setRemote(ORIGIN)
                .setRefSpecs(RefSpec("$BUNDLE_MAIN_REF:$BUNDLE_MAIN_REF"))
                .setCredentialsProvider(credentialsProvider(username, tokenOrPassword))
                .call()
                .flatMap { it.remoteUpdates }

            if (updates.any { it.status in RejectedByNewerCommits }) {
                throw PushRejectedException("the mailbox has changes this device does not have yet")
            }
            val failed = updates.filter { it.status != RemoteRefUpdate.Status.OK && it.status != RemoteRefUpdate.Status.UP_TO_DATE }
            if (failed.isNotEmpty()) {
                error("Push rejected: " + failed.joinToString { listOfNotNull(it.status.name, it.message).joinToString(" ") })
            }
        }
    }

    private suspend fun <T> runGit(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        suspendRunCatching(block)
    }
}

// the mailbox moved on since the last update - another device published first
private val RejectedByNewerCommits = setOf(
    RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD,
    RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED,
)
