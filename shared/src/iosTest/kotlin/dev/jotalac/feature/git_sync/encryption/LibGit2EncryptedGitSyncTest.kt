package dev.jotalac.feature.git_sync.encryption

import dev.jotalac.feature.git_sync.data.LibGit2MailboxRepositoryImpl
import dev.jotalac.feature.git_sync.data.LibGit2MirrorRepositoryImpl
import dev.jotalac.feature.git_sync.data.LibGit2SyncRepositoryImpl
import dev.jotalac.feature.git_sync.data.libgit2.GitRepository
import dev.jotalac.feature.git_sync.data.libgit2.commitAll

class LibGit2EncryptedGitSyncTest : EncryptedGitSyncContractTest() {

    override val gitSync = LibGit2SyncRepositoryImpl()
    override val mirror = LibGit2MirrorRepositoryImpl()
    override val mailbox = LibGit2MailboxRepositoryImpl()

    override fun createRemote(path: String) {
        GitRepository.init(path, initialBranch = "main", bare = true).close()
    }

    override fun initRepository(path: String, branch: String) {
        GitRepository.init(path, initialBranch = branch).close()
    }

    override fun commitAll(path: String) {
        GitRepository.open(path).use { it.commitAll("notes") }
    }
}
