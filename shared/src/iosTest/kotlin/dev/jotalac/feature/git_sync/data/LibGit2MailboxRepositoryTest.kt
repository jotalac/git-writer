package dev.jotalac.feature.git_sync.data

import dev.jotalac.feature.git_sync.data.libgit2.GitRepository

class LibGit2MailboxRepositoryTest : MailboxRepositoryContractTest() {

    override val mailbox = LibGit2MailboxRepositoryImpl()

    override fun createRemote(path: String) {
        GitRepository.init(path, initialBranch = "main", bare = true).close()
    }
}
