package dev.jotalac.feature.git_sync.encryption

import dev.jotalac.feature.git_sync.domain.MailboxRepository
import dev.jotalac.feature.git_sync.domain.MirrorRepository
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/*
* The encrypted sync on real repositories, on every platform: a bare repository stands in for GitHub, folders for
* the laptop and the phone. A local remote needs no credentials, the token is only passed through.
* */
abstract class EncryptedGitSyncContractTest {

    protected abstract val gitSync: GitSyncRepository
    protected abstract val mirror: MirrorRepository
    protected abstract val mailbox: MailboxRepository

    /** An empty bare repository at [path] - the mailbox on GitHub. */
    protected abstract fun createRemote(path: String)

    /** A notebook repository at [path] whose branch is called [branch], like one cloned from an older remote. */
    protected abstract fun initRepository(path: String, branch: String)

    /** Commits everything in the working tree at [path]. */
    protected abstract fun commitAll(path: String)

    private val root = Path(SystemTemporaryDirectory, "encrypted-sync-test-${Random.nextLong().toULong()}")
    private val remote = Path(root, "remote.git").toString()

    // a few iterations keep the tests fast - the derivation itself is the same
    private val encryption by lazy { EncryptedGitSync(gitSync, mirror, mailbox, kdfIterations = 1_000) }

    @BeforeTest
    fun setUp() = SystemFileSystem.createDirectories(root)

    @AfterTest
    fun cleanUp() = deleteRecursively(root)

    // --- getting the notes across

    @Test
    fun `the notes of an encrypted notebook arrive on another device`() = runTest {
        encryptedLaptop("diary.md" to "dear diary")

        val phone = openOnPhone()

        assertEquals("dear diary", read(phone.path, "diary.md"))
    }

    @Test
    fun `edits of different notes on both devices end up on both`() = runTest {
        val laptop = encryptedLaptop("diary.md" to "dear diary")
        val phone = openOnPhone()
        write(laptop.path, "from-laptop.md", "laptop")
        write(phone.path, "from-phone.md", "phone")

        sync(laptop)
        sync(phone)
        sync(laptop)

        for (device in listOf(laptop, phone)) {
            assertEquals("laptop", read(device.path, "from-laptop.md"))
            assertEquals("phone", read(device.path, "from-phone.md"))
        }
    }

    @Test
    fun `the same note edited on both devices is a conflict whose resolution reaches the other device`() = runTest {
        val laptop = encryptedLaptop("diary.md" to "base")
        val phone = openOnPhone()
        write(laptop.path, "diary.md", "laptop")
        write(phone.path, "diary.md", "phone")
        sync(laptop)

        assertEquals(GitSyncStatus.Conflict(setOf("diary.md")), sync(phone))

        gitSync.resolveAllConflicts(phone.path, keepLocalChanges = true).getOrThrow()
        assertEquals(GitSyncStatus.UpToDate, sync(phone))
        sync(laptop)
        assertEquals("phone", read(laptop.path, "diary.md"))
    }

    @Test
    fun `two devices publishing at the same time both get their notes through`() = runTest {
        val laptop = encryptedLaptop("diary.md" to "dear diary")
        val phone = openOnPhone()
        write(laptop.path, "from-laptop.md", "laptop")
        write(phone.path, "from-phone.md", "phone")

        // the laptop publishes right after the phone looked at the mailbox - the phone's upload gets rejected
        var raced = false
        val racingMailbox = object : MailboxRepository by mailbox {
            override suspend fun update(path: String, tokenOrPassword: String, username: String?): Result<Unit> {
                val updated = mailbox.update(path, tokenOrPassword, username)
                if (!raced) {
                    raced = true
                    sync(laptop)
                }
                return updated
            }
        }
        val phoneSync = EncryptedGitSync(gitSync, mirror, racingMailbox, kdfIterations = 1_000)

        val status = phoneSync.sync(phone.path, phone.key, "token", "user").getOrThrow()
        sync(laptop)

        assertTrue(raced)
        assertEquals(GitSyncStatus.UpToDate, status)
        for (device in listOf(laptop, phone)) {
            assertEquals("laptop", read(device.path, "from-laptop.md"))
            assertEquals("phone", read(device.path, "from-phone.md"))
        }
    }

    // --- what the remote shows

    @Test
    fun `the remote holds nothing readable`() = runTest {
        val laptop = encryptedLaptop("secret-plans.md" to "the secret text")
        val phone = openOnPhone()
        write(phone.path, "another-secret.md", "more secret text")
        sync(phone)
        sync(laptop)

        // what anyone with access to the remote sees
        val clone = Path(root, "what-github-shows").toString()
        gitSync.cloneRepository(remote, "token", clone, "user").getOrThrow()
        val files = filesIn(Path(clone)).filterNot { it.startsWith(".git/") }

        assertTrue(ENCRYPTION_CONFIG_FILE in files)
        files.filter { it != ENCRYPTION_CONFIG_FILE }.forEach { file ->
            assertTrue(Regex("""bundles/\d{6}\.bundle\.enc""").matches(file), "$file should not be on the remote")
            assertTrue(readBytes(Path(clone, file)).decodeToString(throwOnInvalidSequence = false).startsWith("GWENC1"))
        }
        files.forEach { file ->
            val content = readBytes(Path(clone, file)).decodeToString(throwOnInvalidSequence = false)
            assertFalse("secret" in content || "secret" in file, "$file shows a note")
        }
    }

    // --- turning it on and opening it

    @Test
    fun `a wrong password opens nothing`() = runTest {
        encryptedLaptop("diary.md" to "dear diary")
        val phone = Path(root, "phone").toString().also { SystemFileSystem.createDirectories(Path(it)) }

        val result = encryption.open(phone, remote, "token", "user", password = "not the password")

        assertIs<WrongPasswordException>(result.exceptionOrNull())
        assertFalse(SystemFileSystem.exists(Path(phone, "diary.md")))
    }

    @Test
    fun `a remote that already has commits is not used for an encrypted notebook`() = runTest {
        // its history would keep the readable notes
        createRemote(remote)
        val other = notebook("other", "main", "readable.md" to "readable")
        gitSync.updateRemoteUrl(other, remote).getOrThrow()
        gitSync.syncNotes(other, "token", "user").getOrThrow()
        val laptop = notebook("laptop", "main", "diary.md" to "dear diary")

        val result = encryption.enable(laptop, remote, "token", "user", PASSWORD)

        assertIs<RemoteNotEmptyException>(result.exceptionOrNull())
    }

    @Test
    fun `a notebook on an older branch name is encrypted with its history`() = runTest {
        // notebooks cloned from older repositories have their notes on master
        createRemote(remote)
        val laptop = notebook("laptop", "master", "diary.md" to "dear diary")
        commitAll(laptop)
        encryption.enable(laptop, remote, "token", "user", PASSWORD).getOrThrow()

        val phone = openOnPhone()

        assertEquals("dear diary", read(phone.path, "diary.md"))
    }

    @Test
    fun `an empty notebook can be encrypted and opened`() = runTest {
        encryptedLaptop()

        val phone = openOnPhone()

        assertEquals(GitSyncStatus.UpToDate, sync(phone))
    }

    // --- helpers

    private class Device(val path: String, val key: NotebookKey)

    /** A laptop notebook with [notes], not committed yet, turned into an encrypted notebook on the empty remote. */
    private suspend fun encryptedLaptop(vararg notes: Pair<String, String>): Device {
        createRemote(remote)
        val path = notebook("laptop", "main", *notes)
        return Device(path, encryption.enable(path, remote, "token", "user", PASSWORD).getOrThrow())
    }

    private suspend fun openOnPhone(): Device {
        val path = Path(root, "phone").toString()
        SystemFileSystem.createDirectories(Path(path))
        return Device(path, encryption.open(path, remote, "token", "user", PASSWORD).getOrThrow())
    }

    private suspend fun sync(device: Device): GitSyncStatus =
        encryption.sync(device.path, device.key, "token", "user").getOrThrow()

    private fun notebook(name: String, branch: String, vararg notes: Pair<String, String>): String {
        val path = Path(root, name).toString()
        SystemFileSystem.createDirectories(Path(path))
        initRepository(path, branch)
        notes.forEach { (note, text) -> write(path, note, text) }
        return path
    }

    private fun write(directory: String, file: String, text: String) =
        SystemFileSystem.sink(Path(directory, file)).buffered().use { it.writeString(text) }

    private fun read(directory: String, file: String): String =
        SystemFileSystem.source(Path(directory, file)).buffered().use { it.readString() }

    private fun readBytes(path: Path): ByteArray = SystemFileSystem.source(path).buffered().use { it.readByteArray() }

    /** The files under [directory], relative to it, with "/" between the parts. */
    private fun filesIn(directory: Path, prefix: String = ""): List<String> =
        SystemFileSystem.list(directory).flatMap { child ->
            val relative = prefix + child.name
            if (SystemFileSystem.metadataOrNull(child)?.isDirectory == true) filesIn(child, "$relative/") else listOf(relative)
        }

    private fun deleteRecursively(path: Path) {
        if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) {
            SystemFileSystem.list(path).forEach(::deleteRecursively)
        }
        SystemFileSystem.delete(path, mustExist = false)
    }

    private companion object {
        const val PASSWORD = "correct horse battery staple"
    }
}
