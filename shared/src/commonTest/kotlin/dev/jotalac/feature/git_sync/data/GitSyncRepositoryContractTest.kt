package dev.jotalac.feature.git_sync.data

import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/*
* What every platform's GitSyncRepository has to do - JGit on desktop and Android, libgit2 on iOS - so both
* run the same tests. Runs against real git repositories in a temp folder: a bare "remote" and clones of it
* standing in for the devices. A local remote needs no credentials, the token is only passed through.
* */
abstract class GitSyncRepositoryContractTest {

    protected abstract val repository: GitSyncRepository

    /** A bare repository at [path] with one commit (README.md) on main - the server. */
    protected abstract fun createRemote(path: String)

    /** A bare repository at [path] without any commit - a server repository that was just created. */
    protected abstract fun createEmptyRemote(path: String)

    /** Commits everything in the working tree at [path], without pushing it. */
    protected abstract fun commitWithoutPushing(path: String)

    private val root = Path(SystemTemporaryDirectory, "git-sync-test-${Random.nextLong().toULong()}")
    private val remote = Path(root, "remote.git").toString()
    private var checkouts = 0

    @BeforeTest
    fun setUp() {
        SystemFileSystem.createDirectories(root)
        createRemote(remote)
    }

    @AfterTest
    fun cleanUp() = deleteRecursively(root)

    // --- sync

    @Test
    fun `sync uploads local notes and downloads the notes of other devices`() = runTest {
        val laptop = device("laptop")
        val phone = device("phone")
        write(phone, "from-phone.md", "phone")
        sync(phone).getOrThrow()

        // not committed - sync commits it
        write(laptop, "from-laptop.md", "laptop")
        val result = sync(laptop)

        assertEquals(GitSyncStatus.UpToDate, result.getOrThrow())
        assertEquals("phone", read(laptop, "from-phone.md"))
        assertEquals("laptop", readFromRemote("from-laptop.md"))
    }

    @Test
    fun `a device without changes of its own gets the edits of the others`() = runTest {
        // only catching up - a fast-forward, no merge
        val laptop = device("laptop")
        val phone = device("phone")
        write(phone, "README.md", "edited on the phone")
        sync(phone).getOrThrow()

        sync(laptop).getOrThrow()

        assertEquals("edited on the phone", read(laptop, "README.md"))
    }

    @Test
    fun `a note deleted on one device is deleted on the others`() = runTest {
        val laptop = device("laptop")
        val phone = device("phone")
        SystemFileSystem.delete(Path(phone, "README.md"))
        sync(phone).getOrThrow()

        sync(laptop).getOrThrow()

        assertFalse(SystemFileSystem.exists(Path(laptop, "README.md")))
    }

    @Test
    fun `sync with nothing new succeeds`() = runTest {
        assertEquals(GitSyncStatus.UpToDate, sync(device("laptop")).getOrThrow())
    }

    @Test
    fun `the first sync of a new notebook uploads it to an empty remote`() = runTest {
        val emptyRemote = Path(root, "empty.git").toString()
        createEmptyRemote(emptyRemote)
        val notebook = Path(root, "notebook").toString()
        SystemFileSystem.createDirectories(Path(notebook))
        repository.initRepository(notebook).getOrThrow()
        write(notebook, "first.md", "first")
        repository.updateRemoteUrl(notebook, emptyRemote).getOrThrow()

        val result = sync(notebook)

        assertEquals(GitSyncStatus.UpToDate, result.getOrThrow())
        assertEquals("first", readFromRemote("first.md", emptyRemote))
    }

    @Test
    fun `a new notebook without notes syncs with an empty remote`() = runTest {
        // nothing to upload yet - there is not even a branch
        val emptyRemote = Path(root, "empty.git").toString()
        createEmptyRemote(emptyRemote)
        val notebook = Path(root, "notebook").toString()
        SystemFileSystem.createDirectories(Path(notebook))
        repository.initRepository(notebook).getOrThrow()
        repository.updateRemoteUrl(notebook, emptyRemote).getOrThrow()

        assertEquals(GitSyncStatus.UpToDate, sync(notebook).getOrThrow())
    }

    @Test
    fun `an empty new notebook connected to a remote gets its notes`() = runTest {
        // no commit at all yet - the branch only exists on the remote
        val notebook = Path(root, "notebook").toString()
        SystemFileSystem.createDirectories(Path(notebook))
        repository.initRepository(notebook).getOrThrow()
        repository.updateRemoteUrl(notebook, remote).getOrThrow()

        val result = sync(notebook)

        assertEquals(GitSyncStatus.UpToDate, result.getOrThrow())
        assertEquals("README", read(notebook, "README.md"))
    }

    // --- push

    @Test
    fun `a push the remote rejects fails instead of reporting success`() = runTest {
        // the remote has a commit the laptop does not, so it refuses the laptop's push
        val laptop = device("laptop")
        val phone = device("phone")
        write(phone, "from-phone.md", "phone")
        sync(phone).getOrThrow()
        write(laptop, "from-laptop.md", "laptop")
        commitWithoutPushing(laptop)

        val result = push(laptop)

        assertTrue(result.isFailure)
        assertFalse(existsOnRemote("from-laptop.md"))
    }

    @Test
    fun `pushing with nothing new succeeds`() = runTest {
        assertTrue(push(device("laptop")).isSuccess)
    }

    // --- conflicts

    @Test
    fun `edits of the same notes on two devices are reported as conflicts`() = runTest {
        val laptop = laptopWithConflictingEdits()

        val result = sync(laptop)

        assertEquals(GitSyncStatus.Conflict(setOf("a.md", "b.md")), result.getOrThrow())
    }

    @Test
    fun `resolving all conflicts keeps the chosen side and uploads it`() = runTest {
        val laptop = laptopWithConflictingEdits()
        sync(laptop)

        repository.resolveAllConflicts(laptop, keepLocalChanges = true).getOrThrow()

        assertEquals("laptop", read(laptop, "a.md"))
        assertEquals("laptop", read(laptop, "b.md"))
        assertEquals(GitSyncStatus.UpToDate, sync(laptop).getOrThrow())
        assertEquals("laptop", readFromRemote("a.md"))
    }

    @Test
    fun `a conflict resolved on its own leaves the other conflicts to resolve`() = runTest {
        val laptop = laptopWithConflictingEdits()
        sync(laptop)

        repository.resolveSingleConflict(laptop, "a.md", keepLocalChanges = true).getOrThrow()
        // b.md is still in conflict, so resolving the rest takes the remote side only for it
        repository.resolveAllConflicts(laptop, keepLocalChanges = false).getOrThrow()

        assertEquals("laptop", read(laptop, "a.md"))
        assertEquals("phone", read(laptop, "b.md"))
        assertEquals(GitSyncStatus.UpToDate, sync(laptop).getOrThrow())
        assertEquals("laptop", readFromRemote("a.md"))
        assertEquals("phone", readFromRemote("b.md"))
    }

    @Test
    fun `aborting a merge brings back the local notes`() = runTest {
        val laptop = laptopWithConflictingEdits()
        sync(laptop)

        repository.abortMerge(laptop).getOrThrow()

        assertEquals("laptop", read(laptop, "a.md"))
        // nothing was resolved - the next sync runs into the same conflicts
        assertEquals(GitSyncStatus.Conflict(setOf("a.md", "b.md")), sync(laptop).getOrThrow())
    }

    /** The phone and the laptop both edited a.md and b.md, the phone synced first. */
    private suspend fun laptopWithConflictingEdits(): String {
        val laptop = device("laptop")
        val phone = device("phone")
        write(phone, "a.md", "base")
        write(phone, "b.md", "base")
        sync(phone).getOrThrow()
        sync(laptop).getOrThrow()

        for (note in listOf("a.md", "b.md")) {
            write(laptop, note, "laptop")
            write(phone, note, "phone")
        }
        sync(phone).getOrThrow()
        return laptop
    }

    // --- helpers

    private suspend fun device(name: String, remote: String = this.remote): String {
        val path = Path(root, name).toString()
        repository.cloneRepository(remote, "token", path, "user").getOrThrow()
        return path
    }

    private suspend fun sync(device: String) = repository.syncNotes(device, "token", "user", "sync")

    private suspend fun push(device: String) = repository.pushChanges(device, "token", "user")

    private suspend fun readFromRemote(note: String, remote: String = this.remote): String =
        read(device("check-${checkouts++}", remote), note)

    private suspend fun existsOnRemote(note: String): Boolean =
        SystemFileSystem.exists(Path(device("check-${checkouts++}"), note))

    private fun write(device: String, note: String, text: String) =
        SystemFileSystem.sink(Path(device, note)).buffered().use { it.writeString(text) }

    private fun read(device: String, note: String): String =
        SystemFileSystem.source(Path(device, note)).buffered().use { it.readString() }

    private fun deleteRecursively(path: Path) {
        if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) {
            SystemFileSystem.list(path).forEach(::deleteRecursively)
        }
        SystemFileSystem.delete(path, mustExist = false)
    }
}
