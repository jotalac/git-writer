package dev.jotalac.feature.git_sync.domain

/**
 * The mailbox of an encrypted notebook: its remote repository, which only holds encrypted bundle files, and the
 * device's copy of it. The copy is how bundles are received (update) and sent (publish).
 */
interface MailboxRepository {

    /** A new copy at [path] of the empty mailbox at [remoteUrl]. */
    suspend fun create(path: String, remoteUrl: String): Result<Unit>

    /** Whether the copy at [path] has nothing published yet - no commit at all. */
    suspend fun isEmpty(path: String): Result<Boolean>

    /** Makes the copy at [path] exactly what the mailbox has. Nothing changes while the mailbox is empty. */
    suspend fun update(path: String, tokenOrPassword: String, username: String?): Result<Unit>

    /**
     * Commits all files of the copy at [path] and pushes them. Fails with [PushRejectedException] when the mailbox has
     * commits the copy does not - another device published first.
     */
    suspend fun publish(path: String, message: String, tokenOrPassword: String, username: String?): Result<Unit>
}

/** The remote refused a push because it has commits the pushing repository does not. */
class PushRejectedException(message: String) : IllegalStateException(message)
