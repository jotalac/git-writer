package dev.jotalac.feature.git_sync.data

import dev.jotalac.feature.git_sync.data.libgit2.GitRepository
import dev.jotalac.feature.git_sync.data.libgit2.commitAll

class LibGit2MirrorRepositoryTest : MirrorRepositoryContractTest() {

    override val mirror = LibGit2MirrorRepositoryImpl()
    override val sync = LibGit2SyncRepositoryImpl()

    override fun commitAll(path: String) {
        GitRepository.open(path).use { it.commitAll("notes") }
    }
}
