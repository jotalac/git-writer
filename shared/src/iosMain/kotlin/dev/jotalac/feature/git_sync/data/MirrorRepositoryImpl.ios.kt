package dev.jotalac.feature.git_sync.data

import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.feature.git_sync.data.libgit2.GitRepository
import dev.jotalac.feature.git_sync.data.libgit2.createBundle
import dev.jotalac.feature.git_sync.data.libgit2.importBundle
import dev.jotalac.feature.git_sync.data.libgit2.mainHead
import dev.jotalac.feature.git_sync.data.libgit2.renameCurrentBranch
import dev.jotalac.feature.git_sync.domain.MirrorRepository
import git2.git_libgit2_init
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.withContext

/** iOS [MirrorRepository] on libgit2 - does what JGitMirrorRepositoryImpl does. */
@OptIn(ExperimentalForeignApi::class)
class LibGit2MirrorRepositoryImpl : MirrorRepository {

    init {
        git_libgit2_init()
    }

    override suspend fun create(path: String): Result<Unit> = runGit {
        GitRepository.init(path, initialBranch = "main", bare = true).close()
    }

    override suspend fun head(path: String): Result<String?> = runGit {
        GitRepository.open(path).use { it.mainHead() }
    }

    override suspend fun createBundle(path: String, since: String?): Result<ByteArray> = runGit {
        GitRepository.open(path).use { it.createBundle(since) }
    }

    override suspend fun importBundle(path: String, bundle: ByteArray): Result<String> = runGit {
        GitRepository.open(path).use { it.importBundle(bundle) }
    }

    override suspend fun useMainBranch(path: String): Result<Unit> = runGit {
        GitRepository.open(path).use { it.renameCurrentBranch("main") }
    }

    private suspend fun <T> runGit(block: () -> T): Result<T> = withContext(EncryptedSyncDispatcher) {
        suspendRunCatching(block)
    }
}
