package dev.jotalac.feature.git_sync.data

import org.eclipse.jgit.api.Git
import java.io.File

class JGitMirrorRepositoryTest : MirrorRepositoryContractTest() {

    override val mirror = JGitMirrorRepositoryImpl()
    override val sync = JGitSyncRepositoryImpl()

    override fun commitAll(path: String) {
        Git.open(File(path)).use { git ->
            git.add().addFilepattern(".").call()
            git.commit().setMessage("notes").call()
        }
    }
}
