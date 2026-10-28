package dev.jotalac.feature.notebooks_management.data

import dev.jotalac.core.data.ActiveNotebookManager
import dev.jotalac.core.utils.deleteRecursively
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.git_sync.encryption.ENCRYPTION_CONFIG_FILE
import dev.jotalac.feature.git_sync.encryption.EncryptedGitSync
import dev.jotalac.feature.git_sync.encryption.NotebookEncryption
import dev.jotalac.feature.git_sync.encryption.PasswordRequiredException
import dev.jotalac.feature.git_sync.encryption.WrongPasswordException
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import dev.jotalac.testing.fakes.FakeMailboxRepository
import dev.jotalac.testing.fakes.FakeMirrorRepository
import dev.jotalac.testing.fakes.FakeGitSyncRepository
import dev.jotalac.testing.fakes.FakeNotebookDao
import dev.jotalac.testing.fakes.InMemoryDataStore
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.writeString

/*
* Runs against real folders in a temporary directory. The database and the preferences are in-memory
* fakes and git is faked - the git side has its own tests (JGitSyncRepositoryTest).
* */
class NotebookRepositoryImplTest {

    private val dao = FakeNotebookDao()
    private val activeNotebookManager = ActiveNotebookManager(InMemoryDataStore())
    private val gitSync = FakeGitSyncRepository()

    /** What the platform reports as the default notebooks folder - on iOS it contains the app container. */
    private var defaultNotebookDirectory = "/notebooks"

    private val repository = NotebookRepositoryImpl(
        notebookDao = dao,
        activeNotebookManager = activeNotebookManager,
        gitSyncRepository = gitSync,
        notebookPathProvider = object : NotebookPathProvider {
            override fun getDefaultNotebookDirectory() = defaultNotebookDirectory
        },
        encryptedGitSync = EncryptedGitSync(gitSync, FakeMirrorRepository(), FakeMailboxRepository()),
    )

    private lateinit var testDir: Path

    @BeforeTest
    fun createTestDir() {
        testDir = Path(SystemTemporaryDirectory, "git_writer_notebooks_test_${Random.nextInt()}")
        SystemFileSystem.createDirectories(testDir)
    }

    @AfterTest
    fun deleteTestDir() {
        testDir.deleteRecursively()
    }

    private fun pathOf(relativePath: String): String = Path(testDir, relativePath).toString()

    private fun exists(path: String): Boolean = SystemFileSystem.exists(Path(path))

    private fun file(relativePath: String): String {
        val path = Path(testDir, relativePath)
        path.parent?.let { SystemFileSystem.createDirectories(it) }
        SystemFileSystem.sink(path).buffered().use { it.writeString("content") }
        return path.toString()
    }

    private fun folder(relativePath: String): String =
        Path(testDir, relativePath).also { SystemFileSystem.createDirectories(it) }.toString()

    private suspend fun createNotebook(name: String): Notebook =
        repository.createNotebook(name, pathOf(name)).getOrThrow()

    // --- creating

    @Test
    fun `a new notebook gets its folder with an images folder and a git repository and an entry`() = runTest {
        val notebook = repository.createNotebook("  Work  ", pathOf("Work")).getOrThrow()

        assertTrue(exists(pathOf("Work/images")))
        assertEquals(listOf(pathOf("Work")), gitSync.initializedPaths)
        assertEquals("Work", notebook.name)
        assertEquals(notebook.id, dao.rows.value.single().id)
    }

    @Test
    fun `a notebook cannot be created in a folder that already exists`() = runTest {
        folder("Work")

        val result = repository.createNotebook("Work", pathOf("Work"))

        assertTrue(result.isFailure)
        assertTrue(dao.rows.value.isEmpty())
        assertTrue(gitSync.initializedPaths.isEmpty())
    }

    @Test
    fun `a failed git setup removes the new folder so creating the notebook again works`() = runTest {
        gitSync.initResult = Result.failure(IllegalStateException("git is broken"))

        val failed = repository.createNotebook("Work", pathOf("Work"))

        assertContains(failed.exceptionOrNull()?.message.orEmpty(), "git is broken")
        assertFalse(exists(pathOf("Work")))

        gitSync.initResult = Result.success(Unit)
        assertTrue(repository.createNotebook("Work", pathOf("Work")).isSuccess)
    }

    @Test
    fun `a notebook cannot be created with the name of another notebook`() = runTest {
        repository.createNotebook("Work", pathOf("elsewhere/Work")).getOrThrow()

        val result = repository.createNotebook("Work", pathOf("Work"))

        assertContains(result.exceptionOrNull()?.message.orEmpty(), "already exists")
        assertFalse(exists(pathOf("Work")))
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `a notebook cannot be cloned with the name of another notebook`() = runTest {
        repository.createNotebook("Team", pathOf("elsewhere/Team")).getOrThrow()

        val result = repository.cloneNotebook("Team", pathOf("Team"), "https://example.com/team.git", "token")

        assertTrue(result.isFailure)
        assertFalse(exists(pathOf("Team")))
        assertTrue(gitSync.clonedDestinations.isEmpty())
    }

    // --- opening an existing folder

    @Test
    fun `a folder with the same name as another notebook is opened under a free name`() = runTest {
        val existing = repository.createNotebook("Diary", pathOf("old/Diary")).getOrThrow()
        folder("Diary")

        val opened = repository.openExistingNotebook(pathOf("Diary")).getOrThrow()

        // a new notebook for the new folder, not the other one returned again
        assertTrue(opened.id != existing.id)
        assertEquals("Diary 2", opened.name)
        assertEquals(pathOf("Diary"), opened.directoryPath)
    }

    @Test
    fun `an opened folder becomes a notebook named after the folder`() = runTest {
        file("Diary/monday.md")

        val notebook = repository.openExistingNotebook(pathOf("Diary")).getOrThrow()

        assertEquals("Diary", notebook.name)
        assertEquals(pathOf("Diary"), notebook.directoryPath)
        assertEquals(listOf(pathOf("Diary")), gitSync.initializedPaths)
    }

    @Test
    fun `a folder that already is a git repository is not initialised again`() = runTest {
        folder("Diary/.git")

        repository.openExistingNotebook(pathOf("Diary")).getOrThrow()

        assertTrue(gitSync.initializedPaths.isEmpty())
    }

    @Test
    fun `opening the same folder twice does not register it twice`() = runTest {
        folder("Diary")

        val first = repository.openExistingNotebook(pathOf("Diary")).getOrThrow()
        val second = repository.openExistingNotebook(pathOf("Diary")).getOrThrow()

        assertEquals(first.id, second.id)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `a failed save does not delete the git history of an opened repository`() = runTest {
        val history = file("Diary/.git/HEAD")
        dao.upsertError = IllegalStateException("database is locked")

        val result = repository.openExistingNotebook(pathOf("Diary"))

        assertTrue(result.isFailure)
        assertTrue(exists(history))
    }

    @Test
    fun `a folder that does not exist cannot be opened`() = runTest {
        assertTrue(repository.openExistingNotebook(pathOf("missing")).isFailure)
        assertTrue(dao.rows.value.isEmpty())
    }

    // --- cloning

    @Test
    fun `invalid credentials stop the clone and remove the empty folder`() = runTest {
        gitSync.validateCredentialsResult = Result.failure(IllegalStateException("not authorized"))

        val result = repository.cloneNotebook("Team", pathOf("Team"), "https://example.com/team.git", "token")

        assertTrue(result.isFailure)
        assertFalse(exists(pathOf("Team")))
        assertTrue(gitSync.clonedDestinations.isEmpty())
    }

    @Test
    fun `a cloned notebook keeps its remote and credentials`() = runTest {
        val notebook = repository.cloneNotebook(
            name = "Team",
            directoryPath = pathOf("Team"),
            remoteUrl = "https://example.com/team.git",
            remotePasswordOrToken = "token",
            remoteUsername = "me",
        ).getOrThrow()

        assertEquals(listOf(pathOf("Team")), gitSync.clonedDestinations)
        val saved = dao.getNotebookById(notebook.id)!!
        assertEquals("https://example.com/team.git", saved.remoteUrl)
        assertEquals("me", saved.remoteUsername)
        assertEquals("token", saved.remotePassword)
    }

    // --- deleting

    @Test
    fun `deleting the open notebook removes its folder and entry and closes it with its note`() = runTest {
        val notebook = createNotebook("Work")
        repository.activateNotebook(notebook.id).getOrThrow()
        repository.activateNote(file("Work/note.md")).getOrThrow()

        repository.deleteNotebook(notebook.id).getOrThrow()

        assertFalse(exists(pathOf("Work")))
        assertTrue(dao.rows.value.isEmpty())
        assertNull(repository.activeNotebookState.first())
        // otherwise the editor would try to open a note from the deleted folder
        assertNull(repository.activeNotePath.first())
    }

    @Test
    fun `deleting another notebook keeps the open one open`() = runTest {
        val open = createNotebook("Work")
        val other = createNotebook("Old")
        repository.activateNotebook(open.id).getOrThrow()

        repository.deleteNotebook(other.id).getOrThrow()

        assertEquals(open.id, repository.activeNotebookState.first()?.id)
    }

    // --- editing

    @Test
    fun `renaming a notebook renames its folder`() = runTest {
        val notebook = createNotebook("Work")

        val renamed = repository.updateNotebook(notebook.id, "Projects", null, null, null).getOrThrow()

        assertFalse(exists(pathOf("Work")))
        assertTrue(exists(pathOf("Projects/images")))
        assertEquals(pathOf("Projects"), renamed.directoryPath)
        assertEquals(pathOf("Projects"), dao.getNotebookById(notebook.id)?.directoryPath)
    }

    @Test
    fun `characters a folder name cannot contain are replaced only in the folder name`() = runTest {
        val notebook = createNotebook("Work")

        val renamed = repository.updateNotebook(notebook.id, "Q1/Q2", null, null, null).getOrThrow()

        assertEquals("Q1/Q2", renamed.name)
        assertEquals(pathOf("Q1_Q2"), renamed.directoryPath)
    }

    @Test
    fun `a notebook cannot take the name of another notebook`() = runTest {
        // in another folder, so only the name is taken - not the folder the rename would create
        repository.createNotebook("Work", pathOf("elsewhere/Work")).getOrThrow()
        val other = createNotebook("Home")

        val result = repository.updateNotebook(other.id, "Work", null, null, null)

        assertTrue(result.isFailure)
        assertTrue(exists(pathOf("Home")))
        assertEquals("Home", dao.getNotebookById(other.id)?.name)
    }

    @Test
    fun `renaming the open notebook keeps its open note open`() = runTest {
        val notebook = createNotebook("Work")
        val note = file("Work/note.md")
        repository.activateNotebook(notebook.id).getOrThrow()
        repository.activateNote(note).getOrThrow()

        repository.updateNotebook(notebook.id, "Projects", null, null, null).getOrThrow()

        assertEquals(pathOf("Projects/note.md"), repository.activeNotePath.first())
    }

    @Test
    fun `new remote credentials are checked before they are saved`() = runTest {
        val notebook = createNotebook("Work")
        gitSync.validateCredentialsResult = Result.failure(IllegalStateException("not authorized"))

        val result = repository.updateNotebook(notebook.id, "Work", "https://example.com/work.git", "me", "bad token")

        assertTrue(result.isFailure)
        assertNull(dao.getNotebookById(notebook.id)?.remoteUrl)
        assertTrue(gitSync.updatedRemoteUrls.isEmpty())
    }

    @Test
    fun `removing the remote also removes it from git`() = runTest {
        val notebook = repository.cloneNotebook("Team", pathOf("Team"), "https://example.com/team.git", "token").getOrThrow()

        repository.updateNotebook(notebook.id, "Team", null, null, null).getOrThrow()

        assertEquals(listOf(pathOf("Team")), gitSync.removedRemotes)
        assertNull(dao.getNotebookById(notebook.id)?.remoteUrl)
    }

    // --- encrypted notebooks

    /** A notebook whose remote is an encrypted notebook's. */
    private suspend fun encryptedNotebook(): Notebook {
        val notebook = createNotebook("Diary")
        val stored = dao.getNotebookById(notebook.id)!!
        dao.upsertNotebook(
            stored.copy(
                remoteUrl = "https://example.com/diary.git", remoteUsername = "me", remotePassword = "token",
                isEncrypted = true, encryptionKey = "key",
            )
        )
        return notebook
    }

    /** Clones look like the remote of an encrypted notebook with [password]. */
    private suspend fun encryptedRemote(password: String = "correct horse") {
        val config = NotebookEncryption.create(password, iterations = 1_000).first.toJson()
        gitSync.onClone = { destination ->
            SystemFileSystem.createDirectories(Path(destination))
            SystemFileSystem.sink(Path(destination, ENCRYPTION_CONFIG_FILE)).buffered().use { it.writeString(config) }
        }
    }

    @Test
    fun `a new token for an encrypted notebook keeps its git remote on the mirror`() = runTest {
        // its git remote is the local mirror - pointing it at the remote repository would break the encryption
        val notebook = encryptedNotebook()

        repository.updateNotebook(notebook.id, "Diary", "https://example.com/diary.git", "me", "new token").getOrThrow()

        assertTrue(gitSync.updatedRemoteUrls.isEmpty())
        val saved = dao.getNotebookById(notebook.id)!!
        assertEquals("new token", saved.remotePassword)
        assertTrue(saved.isEncrypted)
        assertEquals("key", saved.encryptionKey)
    }

    @Test
    fun `the remote of an encrypted notebook can be neither changed nor removed`() = runTest {
        // its bundles are there
        val notebook = encryptedNotebook()

        assertTrue(repository.updateNotebook(notebook.id, "Diary", "https://example.com/other.git", "me", "token").isFailure)
        assertTrue(repository.updateNotebook(notebook.id, "Diary", null, null, null).isFailure)

        assertTrue(gitSync.removedRemotes.isEmpty())
        assertEquals("https://example.com/diary.git", dao.getNotebookById(notebook.id)?.remoteUrl)
    }

    @Test
    fun `an encrypted remote is not added without its password`() = runTest {
        encryptedRemote()

        val result = repository.cloneNotebook("Diary", pathOf("Diary"), "https://example.com/diary.git", "token")

        assertIs<PasswordRequiredException>(result.exceptionOrNull())
        assertFalse(exists(pathOf("Diary")))
        assertNull(dao.getNotebookByName("Diary"))
    }

    @Test
    fun `an encrypted remote is not added with a wrong password`() = runTest {
        encryptedRemote(password = "correct horse")

        val result = repository.cloneNotebook(
            "Diary", pathOf("Diary"), "https://example.com/diary.git", "token", encryptionPassword = "battery staple",
        )

        assertIs<WrongPasswordException>(result.exceptionOrNull())
        assertFalse(exists(pathOf("Diary")))
        assertNull(dao.getNotebookByName("Diary"))
    }

    @Test
    fun `an encrypted remote opens with its password and the notebook keeps its key`() = runTest {
        encryptedRemote(password = "correct horse")
        gitSync.syncResult = Result.success(GitSyncStatus.UpToDate)

        val notebook = repository.cloneNotebook(
            "Diary", pathOf("Diary"), "https://example.com/diary.git", "token", encryptionPassword = "correct horse",
        ).getOrThrow()

        val saved = dao.getNotebookById(notebook.id)!!
        assertTrue(saved.isEncrypted)
        assertNotNull(saved.encryptionKey)
        // the notebook's git remote is the mirror inside it, not the remote repository
        assertTrue(gitSync.updatedRemoteUrls.all { (path, url) -> path == pathOf("Diary") && url.endsWith("mirror.git") })
    }

    // --- the open notebook and note

    @Test
    fun `a notebook whose folder was deleted cannot be opened and the open one stays open`() = runTest {
        val work = createNotebook("Work")
        val gone = createNotebook("Gone")
        repository.activateNotebook(work.id).getOrThrow()
        Path(pathOf("Gone")).deleteRecursively()

        val result = repository.activateNotebook(gone.id)

        assertTrue(result.isFailure)
        assertEquals(work.id, repository.activeNotebookState.first()?.id)
    }

    @Test
    fun `switching notebooks closes the note of the previous one`() = runTest {
        val work = createNotebook("Work")
        val home = createNotebook("Home")
        repository.activateNotebook(work.id).getOrThrow()
        repository.activateNote(file("Work/note.md")).getOrThrow()

        repository.activateNotebook(home.id).getOrThrow()

        assertNull(repository.activeNotePath.first())
    }

    @Test
    fun `moving a folder moves the open note inside it along`() = runTest {
        repository.activateNote(pathOf("nb/ideas/note.md")).getOrThrow()

        repository.syncActiveNotePathOnMoved(pathOf("nb/ideas"), pathOf("nb/archive/ideas")).getOrThrow()

        assertEquals(pathOf("nb/archive/ideas/note.md"), repository.activeNotePath.first())
    }

    @Test
    fun `deleting a folder closes the note inside it but not one in a folder with a longer name`() = runTest {
        val note = pathOf("nb/notes2/note.md")
        repository.activateNote(note).getOrThrow()

        repository.syncActiveNotePathOnDeleted(pathOf("nb/notes")).getOrThrow()
        assertEquals(note, repository.activeNotePath.first())

        repository.syncActiveNotePathOnDeleted(pathOf("nb/notes2")).getOrThrow()
        assertNull(repository.activeNotePath.first())
    }

    // --- iOS: the app folder changes with every install

    private fun iosContainer(installId: String) = "/var/mobile/Containers/Data/Application/$installId/"
    private val firstInstall = "11111111-2222-3333-4444-555555555555"
    private val reinstall = "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"

    @Test
    fun `on iOS the open note is found again after the app is reinstalled`() = runTest {
        defaultNotebookDirectory = iosContainer(firstInstall) + "Documents/git-writer-notes"
        repository.activateNote(iosContainer(firstInstall) + "Documents/git-writer-notes/nb/note.md").getOrThrow()

        defaultNotebookDirectory = iosContainer(reinstall) + "Documents/git-writer-notes"

        assertEquals(iosContainer(reinstall) + "Documents/git-writer-notes/nb/note.md", repository.activeNotePath.first())
    }

    @Test
    fun `on iOS saved notebooks point into the current app folder`() = runTest {
        // stored without the container part, the way the repository saves it
        dao.upsertNotebook(NotebookEntity(0, "nb", "Documents/git-writer-notes/nb", null, null, null))
        defaultNotebookDirectory = iosContainer(reinstall) + "Documents/git-writer-notes"

        assertEquals(
            iosContainer(reinstall) + "Documents/git-writer-notes/nb",
            repository.getAllNotebooks().first().single().directoryPath,
        )
    }
}
