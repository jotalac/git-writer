package dev.jotalac.feature.git_sync.encryption

import dev.jotalac.feature.git_sync.data.JGitMailboxRepositoryImpl
import dev.jotalac.feature.git_sync.data.JGitMirrorRepositoryImpl
import dev.jotalac.feature.git_sync.data.JGitSyncRepositoryImpl
import org.eclipse.jgit.api.Git
import java.io.File

class JGitEncryptedGitSyncTest : EncryptedGitSyncContractTest() {

    override val gitSync = JGitSyncRepositoryImpl()
    override val mirror = JGitMirrorRepositoryImpl()
    override val mailbox = JGitMailboxRepositoryImpl()

    override fun createRemote(path: String) {
        Git.init().setBare(true).setDirectory(File(path)).setInitialBranch("main").call().close()
    }

    override fun initRepository(path: String, branch: String) {
        Git.init().setDirectory(File(path)).setInitialBranch(branch).call().close()
    }

    override fun commitAll(path: String) {
        Git.open(File(path)).use { git ->
            git.add().addFilepattern(".").call()
            git.commit().setMessage("notes").call()
        }
    }
}
