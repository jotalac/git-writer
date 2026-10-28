package dev.jotalac.feature.editor.ui

import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.domain.GitConflictResolutionStrategy
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.feature.editor.domain.EditorTabItem
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.git_sync.encryption.EncryptedGitSync
import dev.jotalac.feature.git_sync.encryption.NotebookKey
import dev.jotalac.testing.fakes.FakeMailboxRepository
import dev.jotalac.testing.fakes.FakeMirrorRepository
import dev.jotalac.testing.fakes.FakeGitSyncRepository
import dev.jotalac.testing.fakes.FakeNotebookRepository
import dev.jotalac.testing.fakes.FakeUserSettings
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.err_failed_resolve_conflict
import git_writer.shared.generated.resources.err_failed_resolve_conflicts
import git_writer.shared.generated.resources.err_failed_sync
import git_writer.shared.generated.resources.err_push_after_resolve
import git_writer.shared.generated.resources.msg_conflicts_resolved
import git_writer.shared.generated.resources.msg_notes_synced
import git_writer.shared.generated.resources.msg_sync_aborted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditorSyncControllerTest {

    private val gitSync = FakeGitSyncRepository()
    private val mailbox = FakeMailboxRepository()
    private val notebooks = FakeNotebookRepository()
    private val snackbar = SnackbarManager()
    private val settings = FakeUserSettings()

    private val openNote = "/notebook/note.md"
    private val state = MutableStateFlow(
        EditorScreenState(openedTabs = listOf(EditorTabItem(id = 0L, notePath = openNote)))
    )

    private var saveCount = 0
    private var savedBeforeSync = false
    private val reloadedNotes = mutableListOf<String>()

    private val controller = EditorSyncController(
        gitSyncRepository = gitSync,
        encryptedGitSync = EncryptedGitSync(gitSync, FakeMirrorRepository(), mailbox),
        notebookRepository = notebooks,
        snackbarManager = snackbar,
        userSettingsManager = settings,
        state = state,
        saveCurrentNote = {
            saveCount++
            savedBeforeSync = gitSync.syncedPaths.isEmpty()
        },
        reloadNote = { reloadedNotes += it },
    )

    private fun activateNotebook(
        remoteUrl: String? = "https://example.com/notes.git",
        password: String? = "token",
        encryptionKey: String? = null,
    ) {
        notebooks.activeNotebook.value = Notebook(
            id = 1,
            name = "notebook",
            directoryPath = "/notebook",
            remoteUrl = remoteUrl,
            remoteUsername = "user",
            remotePassword = password,
            isEncrypted = encryptionKey != null,
            encryptionKey = encryptionKey,
        )
    }

    private fun conflictStrategy(strategy: GitConflictResolutionStrategy) {
        settings.settings.value = UserSettingsState(gitConflictStrategy = strategy)
    }

    private fun inConflict(vararg files: String) {
        state.value = state.value.copy(
            conflictedFiles = files.toList(),
            gitSyncStatus = GitSyncStatus.Conflict(files.toSet()),
        )
    }

    /** Subscribes before anything is triggered: the flow does not replay, so a late read would miss it. */
    private fun TestScope.collectSnackbars(): List<SnackbarText> {
        val received = mutableListOf<SnackbarText>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            snackbar.messages.collect { received += it }
        }
        return received
    }

    // --- sync

    @Test
    fun `sync does nothing for a notebook without a remote`() = runTest {
        activateNotebook(remoteUrl = null)

        controller.sync()

        assertTrue(gitSync.syncedPaths.isEmpty())
        assertEquals(0, saveCount)
        assertEquals(GitSyncStatus.UpToDate, state.value.gitSyncStatus)
    }

    @Test
    fun `an encrypted notebook is synced through its encrypted remote`() = runTest {
        activateNotebook(encryptionKey = NotebookKey(ByteArray(32)).encoded)
        gitSync.syncResult = Result.success(GitSyncStatus.UpToDate)

        controller.sync()

        // the mailbox is downloaded first, then the plain sync runs against the mirror
        assertEquals(listOf("/notebook/.git/gitwriter/mailbox"), mailbox.updatedPaths.map { it.replace('\\', '/') })
        assertEquals(listOf("/notebook"), gitSync.syncedPaths)
        assertEquals(GitSyncStatus.UpToDate, state.value.gitSyncStatus)
    }

    @Test
    fun `an encrypted notebook without its key on this device is not synced`() = runTest {
        notebooks.activeNotebook.value = Notebook(
            1, "notebook", "/notebook", "https://example.com/notes.git", "user", "token", isEncrypted = true,
        )
        val messages = collectSnackbars()

        controller.sync()

        assertTrue(gitSync.syncedPaths.isEmpty())
        assertEquals(GitSyncStatus.GitSyncFailed, state.value.gitSyncStatus)
        assertEquals(1, messages.size)
    }

    @Test
    fun `a resolved conflict of an encrypted notebook is published through its encrypted remote`() = runTest {
        // a plain push would go to the mirror only - publishing it is the encrypted sync's job
        activateNotebook(encryptionKey = NotebookKey(ByteArray(32)).encoded)
        gitSync.syncResult = Result.success(GitSyncStatus.Conflict(setOf("diary.md")))
        controller.sync()
        gitSync.syncResult = Result.success(GitSyncStatus.UpToDate)

        controller.resolveAllConflicts(keepLocal = true)

        assertTrue(gitSync.pushedPaths.isEmpty())
        assertEquals(2, mailbox.updatedPaths.size)
        assertEquals(GitSyncStatus.UpToDate, state.value.gitSyncStatus)
    }

    @Test
    fun `sync saves the open note before syncing`() = runTest {
        // otherwise the latest edits would not be part of the commit
        activateNotebook()
        gitSync.syncResult = Result.success(GitSyncStatus.UpToDate)

        controller.sync()

        assertEquals(1, saveCount)
        assertTrue(savedBeforeSync)
        assertEquals(listOf("/notebook"), gitSync.syncedPaths)
    }

    @Test
    fun `a successful sync reloads the open note with the pulled changes`() = runTest {
        activateNotebook()
        gitSync.syncResult = Result.success(GitSyncStatus.UpToDate)
        val messages = collectSnackbars()

        controller.sync()

        assertEquals(listOf(openNote), reloadedNotes)
        assertEquals(GitSyncStatus.UpToDate, state.value.gitSyncStatus)
        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.msg_notes_synced)), messages)
    }

    @Test
    fun `a failed sync is reported with its cause`() = runTest {
        activateNotebook()
        gitSync.syncResult = Result.failure(IllegalStateException("network unreachable"))
        val messages = collectSnackbars()

        controller.sync()

        assertEquals(GitSyncStatus.GitSyncFailed, state.value.gitSyncStatus)
        assertEquals(
            listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_sync, "network unreachable")),
            messages,
        )
    }

    // --- conflicts found by sync

    @Test
    fun `with manual resolution the conflicts are shown to the user`() = runTest {
        activateNotebook()
        conflictStrategy(GitConflictResolutionStrategy.MANUAL)
        gitSync.syncResult = Result.success(GitSyncStatus.Conflict(setOf("a.md", "b.md")))

        controller.sync()

        assertEquals(listOf("a.md", "b.md"), state.value.conflictedFiles)
        assertEquals(GitSyncStatus.Conflict(setOf("a.md", "b.md")), state.value.gitSyncStatus)
        assertTrue(gitSync.resolvedAllConflicts.isEmpty())
        assertTrue(gitSync.pushedPaths.isEmpty())
    }

    @Test
    fun `with the local strategy conflicts are resolved by keeping local changes and pushed`() = runTest {
        activateNotebook()
        conflictStrategy(GitConflictResolutionStrategy.LOCAL)
        gitSync.syncResult = Result.success(GitSyncStatus.Conflict(setOf("a.md")))

        controller.sync()

        assertEquals(listOf(true), gitSync.resolvedAllConflicts)
        assertEquals(listOf("/notebook"), gitSync.pushedPaths)
        assertTrue(state.value.conflictedFiles.isEmpty())
        assertEquals(GitSyncStatus.UpToDate, state.value.gitSyncStatus)
        assertEquals(listOf(openNote), reloadedNotes)
    }

    @Test
    fun `with the remote strategy conflicts are resolved by taking the remote changes`() = runTest {
        activateNotebook()
        conflictStrategy(GitConflictResolutionStrategy.REMOTE)
        gitSync.syncResult = Result.success(GitSyncStatus.Conflict(setOf("a.md")))

        controller.sync()

        assertEquals(listOf(false), gitSync.resolvedAllConflicts)
    }

    // --- resolving conflicts one by one

    @Test
    fun `resolving one of several conflicts keeps the others and does not push yet`() = runTest {
        activateNotebook()
        inConflict("a.md", "b.md")

        controller.resolveSingleConflict("a.md", keepLocal = true)

        assertEquals(listOf("a.md" to true), gitSync.resolvedConflicts)
        assertEquals(listOf("b.md"), state.value.conflictedFiles)
        assertTrue(state.value.gitSyncStatus is GitSyncStatus.Conflict)
        assertTrue(gitSync.pushedPaths.isEmpty())
    }

    @Test
    fun `resolving the last conflict pushes and marks the notebook up to date`() = runTest {
        activateNotebook()
        inConflict("a.md")
        val messages = collectSnackbars()

        controller.resolveSingleConflict("a.md", keepLocal = false)

        assertEquals(listOf("/notebook"), gitSync.pushedPaths)
        assertTrue(state.value.conflictedFiles.isEmpty())
        assertEquals(GitSyncStatus.UpToDate, state.value.gitSyncStatus)
        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.msg_conflicts_resolved)), messages)
    }

    @Test
    fun `resolving a conflict in the open note reloads it`() = runTest {
        // git reports paths relative to the notebook, the editor has the full path
        activateNotebook()
        inConflict("note.md", "other.md")

        controller.resolveSingleConflict("note.md", keepLocal = false)

        assertEquals(listOf(openNote), reloadedNotes)
    }

    @Test
    fun `resolving a conflict in another note leaves the open note alone`() = runTest {
        activateNotebook()
        inConflict("note.md", "other.md")

        controller.resolveSingleConflict("other.md", keepLocal = false)

        assertTrue(reloadedNotes.isEmpty())
    }

    @Test
    fun `a failed resolution keeps the conflict and reports the cause`() = runTest {
        activateNotebook()
        inConflict("a.md")
        gitSync.resolveConflictResult = Result.failure(IllegalStateException("file locked"))
        val messages = collectSnackbars()

        controller.resolveSingleConflict("a.md", keepLocal = true)

        assertEquals(listOf("a.md"), state.value.conflictedFiles)
        assertTrue(gitSync.pushedPaths.isEmpty())
        assertEquals(
            listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_resolve_conflict, "file locked")),
            messages,
        )
    }

    // --- pushing the resolved conflicts

    @Test
    fun `a failed push after resolving all conflicts is reported instead of shown as synced`() = runTest {
        activateNotebook()
        conflictStrategy(GitConflictResolutionStrategy.LOCAL)
        gitSync.syncResult = Result.success(GitSyncStatus.Conflict(setOf("a.md")))
        gitSync.pushResult = Result.failure(IllegalStateException("Push rejected: origin REJECTED_NONFASTFORWARD"))
        val messages = collectSnackbars()

        controller.sync()

        // the conflicts are resolved locally, only the upload is missing
        assertTrue(state.value.conflictedFiles.isEmpty())
        assertEquals(GitSyncStatus.GitSyncFailed, state.value.gitSyncStatus)
        assertEquals(
            listOf<SnackbarText>(
                SnackbarText.message(Res.string.err_push_after_resolve, "Push rejected: origin REJECTED_NONFASTFORWARD")
            ),
            messages,
        )
    }

    @Test
    fun `a failed push after resolving the last conflict is reported instead of shown as synced`() = runTest {
        activateNotebook()
        inConflict("a.md")
        gitSync.pushResult = Result.failure(IllegalStateException("not authorized"))
        val messages = collectSnackbars()

        controller.resolveSingleConflict("a.md", keepLocal = true)

        assertTrue(state.value.conflictedFiles.isEmpty())
        assertEquals(GitSyncStatus.GitSyncFailed, state.value.gitSyncStatus)
        assertEquals(
            listOf<SnackbarText>(SnackbarText.message(Res.string.err_push_after_resolve, "not authorized")),
            messages,
        )
    }

    // --- resolving all / aborting

    @Test
    fun `a failed resolve all marks the sync as failed`() = runTest {
        activateNotebook()
        inConflict("a.md", "b.md")
        gitSync.resolveConflictResult = Result.failure(IllegalStateException("merge broken"))
        val messages = collectSnackbars()

        controller.resolveAllConflicts(keepLocal = true)

        assertEquals(GitSyncStatus.GitSyncFailed, state.value.gitSyncStatus)
        assertTrue(gitSync.pushedPaths.isEmpty())
        assertEquals(
            listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_resolve_conflicts, "merge broken")),
            messages,
        )
    }

    @Test
    fun `aborting the merge clears the conflicts`() = runTest {
        activateNotebook()
        inConflict("a.md", "b.md")
        val messages = collectSnackbars()

        controller.abort()

        assertEquals(listOf("/notebook"), gitSync.abortedMerges)
        assertTrue(state.value.conflictedFiles.isEmpty())
        assertEquals(GitSyncStatus.UpToDate, state.value.gitSyncStatus)
        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.msg_sync_aborted)), messages)
    }
}
