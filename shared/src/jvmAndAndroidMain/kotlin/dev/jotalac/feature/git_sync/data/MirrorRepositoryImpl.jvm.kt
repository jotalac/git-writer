package dev.jotalac.feature.git_sync.data

import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.feature.git_sync.domain.MirrorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.NullProgressMonitor
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.BundleWriter
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.TransportBundleStream
import org.eclipse.jgit.transport.URIish
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

private const val MAIN_BRANCH = "main"

class JGitMirrorRepositoryImpl : MirrorRepository {

    override suspend fun create(path: String): Result<Unit> = runGit {
        Git.init().setBare(true).setDirectory(File(path)).setInitialBranch(MAIN_BRANCH).call().close()
    }

    override suspend fun head(path: String): Result<String?> = runGit {
        Git.open(File(path)).use { it.repository.resolve(BUNDLE_MAIN_REF)?.name }
    }

    override suspend fun createBundle(path: String, since: String?): Result<ByteArray> = runGit {
        Git.open(File(path)).use { git ->
            val repository = git.repository
            val head = repository.resolve(BUNDLE_MAIN_REF) ?: error("main has no commits to bundle")
            val writer = BundleWriter(repository)
            writer.include(BUNDLE_MAIN_REF, head)
            if (since != null) {
                RevWalk(repository).use { walk -> writer.assume(walk.parseCommit(ObjectId.fromString(since))) }
            }
            ByteArrayOutputStream().use { out ->
                writer.writeBundle(NullProgressMonitor.INSTANCE, out)
                out.toByteArray()
            }
        }
    }

    override suspend fun importBundle(path: String, bundle: ByteArray): Result<String> = runGit {
        Git.open(File(path)).use { git ->
            val repository = git.repository
            TransportBundleStream(repository, URIish("bundle"), ByteArrayInputStream(bundle)).use { transport ->
                transport.fetch(NullProgressMonitor.INSTANCE, listOf(RefSpec("+$BUNDLE_MAIN_REF:$BUNDLE_MAIN_REF")))
            }
            repository.resolve(BUNDLE_MAIN_REF)?.name ?: error("the bundle does not carry $BUNDLE_MAIN_REF")
        }
    }

    override suspend fun useMainBranch(path: String): Result<Unit> = runGit {
        Git.open(File(path)).use { git ->
            val repository = git.repository
            val current = repository.branch
            when {
                current == MAIN_BRANCH -> Unit
                // no commit yet - only HEAD names the branch
                repository.resolve(Constants.HEAD) == null -> repository.updateRef(Constants.HEAD).link(BUNDLE_MAIN_REF)
                else -> git.branchRename().setOldName(current).setNewName(MAIN_BRANCH).call()
            }
        }
    }

    private suspend fun <T> runGit(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        suspendRunCatching(block)
    }
}
