package dev.jotalac.testing.fakes

import dev.jotalac.feature.git_sync.domain.MirrorRepository

/** A mirror that stays empty - an encrypted sync over it finds nothing new to publish. */
class FakeMirrorRepository : MirrorRepository {
    override suspend fun create(path: String) = Result.success(Unit)
    override suspend fun head(path: String): Result<String?> = Result.success(null)
    override suspend fun createBundle(path: String, since: String?) = Result.success(ByteArray(0))
    override suspend fun importBundle(path: String, bundle: ByteArray) = Result.success("head")
    override suspend fun useMainBranch(path: String) = Result.success(Unit)
}
