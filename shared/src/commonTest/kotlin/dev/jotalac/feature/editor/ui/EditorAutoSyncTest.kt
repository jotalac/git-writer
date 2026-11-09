package dev.jotalac.feature.editor.ui

import androidx.lifecycle.viewModelScope
import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.utils.RemoteStatusMonitor
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.git_sync.encryption.EncryptedGitSync
import dev.jotalac.testing.fakes.FakeEditorRepository
import dev.jotalac.testing.fakes.FakeMailboxRepository
import dev.jotalac.testing.fakes.FakeMirrorRepository
import dev.jotalac.testing.fakes.FakeGitSyncRepository
import dev.jotalac.testing.fakes.FakeNotebookRepository
import dev.jotalac.testing.fakes.FakeUserSettings
import dev.jotalac.testing.viewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/*
* Auto sync runs on virtual time: the 10 minutes and the 3 s typing pause pass instantly. The periodic loop
* never ends on its own, so every test stops its ViewModel at the end.
* */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorAutoSyncTest {

    private val editor = FakeEditorRepository()
    private val notebooks = FakeNotebookRepository()
    private val settings = FakeUserSettings(UserSettingsState(autoSync = true))
    private val gitSync = FakeGitSyncRepository().apply { syncResult = Result.success(GitSyncStatus.UpToDate) }
    // what the OS reports about the connection - the remote itself always answers
    private val network = MutableStateFlow<Boolean?>(true)

    private val notebbokoWithRemote = Notebook(1, "notes", "/notes", "https://github.com/me/notes.git", "me", "token")

    private fun autoSyncTest(block: suspend TestScope.(EditorViewModel) -> Unit) = viewModelTest {
        val viewModel = EditorViewModel(
            notebooks, editor, SnackbarManager(), settings, gitSync,
            RemoteStatusMonitor({ true }, { network }),
            EncryptedGitSync(gitSync, FakeMirrorRepository(), FakeMailboxRepository()),
        )
        try {
            block(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
            // let the cancellation finish while Main is still the test dispatcher (it is reset right after)
            advanceUntilIdle()
        }
    }

    private fun TestScope.wait(duration: Duration) {
        advanceTimeBy(duration)
        runCurrent()
    }

    @Test
    fun `a notebook with a remote is synced right when it is opened`() = autoSyncTest {
        notebooks.activeNotebook.value = notebbokoWithRemote

        // no waiting - nobody is typing
        runCurrent()

        assertEquals(listOf("/notes"), gitSync.syncedPaths)
    }

    @Test
    fun `it syncs again every 10 minutes`() = autoSyncTest {
        notebooks.activeNotebook.value = notebbokoWithRemote
        wait(4.seconds)

        wait(10.minutes + 4.seconds)
        wait(10.minutes + 4.seconds)

        assertEquals(3, gitSync.syncedPaths.size)
    }

    @Test
    fun `nothing is synced with auto sync off`() = autoSyncTest {
        settings.settings.value = UserSettingsState(autoSync = false)
        notebooks.activeNotebook.value = notebbokoWithRemote

        wait(30.minutes)

        assertTrue(gitSync.syncedPaths.isEmpty())
    }

    @Test
    fun `a notebook without a remote is not synced`() = autoSyncTest {
        notebooks.activeNotebook.value = notebbokoWithRemote.copy(remoteUrl = null, remotePassword = null)

        wait(30.minutes)

        assertTrue(gitSync.syncedPaths.isEmpty())
    }

    @Test
    fun `turning auto sync off stops the periodic sync`() = autoSyncTest {
        notebooks.activeNotebook.value = notebbokoWithRemote
        wait(4.seconds)

        settings.settings.value = UserSettingsState(autoSync = false)
        wait(30.minutes)

        assertEquals(1, gitSync.syncedPaths.size)
    }

    @Test
    fun `nothing is synced offline and it syncs right when the device is back online`() = autoSyncTest {
        network.value = false
        notebooks.activeNotebook.value = notebbokoWithRemote
        wait(30.minutes)
        assertTrue(gitSync.syncedPaths.isEmpty())

        network.value = true
        runCurrent()

        assertEquals(listOf("/notes"), gitSync.syncedPaths)
    }

    @Test
    fun `going offline stops the periodic sync`() = autoSyncTest {
        notebooks.activeNotebook.value = notebbokoWithRemote
        runCurrent()

        network.value = false
        wait(30.minutes)

        assertEquals(1, gitSync.syncedPaths.size)
    }

    @Test
    fun `opening another notebook syncs it right away`() = autoSyncTest {
        notebooks.activeNotebook.value = notebbokoWithRemote
        wait(4.seconds)

        notebooks.activeNotebook.value = notebbokoWithRemote.copy(id = 2, directoryPath = "/work")
        runCurrent()

        assertEquals(listOf("/notes", "/work"), gitSync.syncedPaths)
    }

    @Test
    fun `typing postpones the automatic sync until it pauses`() = autoSyncTest { viewModel ->
        notebooks.activeNotebook.value = notebbokoWithRemote
        runCurrent()

        // typing (an edit every second) just as the next sync is due
        wait(10.minutes - 1.seconds)
        repeat(5) {
            viewModel.onAction(EditorAction.UpdateText("typing $it"))
            wait(1.seconds)
        }
        assertEquals(1, gitSync.syncedPaths.size)

        // 3 s after the last edit
        wait(3.seconds)
        assertEquals(2, gitSync.syncedPaths.size)
    }

    @Test
    fun `leaving the app syncs once more`() = autoSyncTest { viewModel ->
        notebooks.activeNotebook.value = notebbokoWithRemote
        wait(4.seconds)

        viewModel.onAction(EditorAction.SyncBeforeLeaving)
        runCurrent()

        assertEquals(2, gitSync.syncedPaths.size)
    }

    @Test
    fun `leaving the app does not sync with auto sync off`() = autoSyncTest { viewModel ->
        settings.settings.value = UserSettingsState(autoSync = false)
        notebooks.activeNotebook.value = notebbokoWithRemote
        runCurrent()

        viewModel.onAction(EditorAction.SyncBeforeLeaving)
        runCurrent()

        assertTrue(gitSync.syncedPaths.isEmpty())
    }

    @Test
    fun `leaving the app offline does not sync`() = autoSyncTest { viewModel ->
        network.value = false
        notebooks.activeNotebook.value = notebbokoWithRemote
        runCurrent()

        viewModel.onAction(EditorAction.SyncBeforeLeaving)
        runCurrent()

        assertTrue(gitSync.syncedPaths.isEmpty())
    }

    @Test
    fun `the open note is reloaded after a sync only when the pull changed it`() = autoSyncTest { viewModel ->
        notebooks.activeNotebook.value = notebbokoWithRemote
        notebooks.activeNote.value = "/notes/note.md"
        editor.existingPaths += "/notes/note.md"
        // what is on disk - also after the sync saved the note, nothing was pulled
        editor.readResult = Result.success("# Note\n\ntext")
        wait(4.seconds)
        // opened, then compared with the disk after the sync
        val readsAfterFirstSync = editor.readPaths.size

        // the next sync pulls a change to the open note
        editor.readResult = Result.success("# Note\n\ntext\n\nfrom another device")
        wait(10.minutes + 4.seconds)

        assertEquals(2, readsAfterFirstSync, "opened once, not reloaded after a sync without changes")
        assertEquals(4, editor.readPaths.size, "compared, then reloaded after the sync that changed it")
        assertEquals("# Note\n\ntext\n\nfrom another device", viewModel.noteText.value)
    }
}
