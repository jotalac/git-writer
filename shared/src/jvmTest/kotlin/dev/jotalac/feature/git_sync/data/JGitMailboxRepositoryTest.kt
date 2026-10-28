package dev.jotalac.feature.git_sync.data

import org.eclipse.jgit.api.Git
import java.io.File

class JGitMailboxRepositoryTest : MailboxRepositoryContractTest() {

    override val mailbox = JGitMailboxRepositoryImpl()

    override fun createRemote(path: String) {
        Git.init().setBare(true).setDirectory(File(path)).setInitialBranch("main").call().close()
    }
}
