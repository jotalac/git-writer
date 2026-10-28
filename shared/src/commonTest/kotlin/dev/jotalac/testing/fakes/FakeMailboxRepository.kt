package dev.jotalac.testing.fakes

import dev.jotalac.feature.git_sync.domain.MailboxRepository

/** A mailbox that was published to already and has no new bundles. */
class FakeMailboxRepository : MailboxRepository {

    var updateResult: Result<Unit> = Result.success(Unit)

    /** path of every mailbox copy that was brought up to date */
    val updatedPaths = mutableListOf<String>()

    override suspend fun create(path: String, remoteUrl: String) = Result.success(Unit)
    override suspend fun isEmpty(path: String) = Result.success(false)

    override suspend fun update(path: String, tokenOrPassword: String, username: String?): Result<Unit> {
        updatedPaths += path
        return updateResult
    }

    override suspend fun publish(path: String, message: String, tokenOrPassword: String, username: String?) =
        Result.success(Unit)
}
