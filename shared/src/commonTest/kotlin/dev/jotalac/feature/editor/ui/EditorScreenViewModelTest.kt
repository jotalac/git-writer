package dev.jotalac.feature.editor.ui

import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.utils.RemoteStatus
import dev.jotalac.core.utils.RemoteStatusMonitor
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.git_sync.encryption.EncryptedGitSync
import dev.jotalac.testing.fakes.FakeEditorRepository
import dev.jotalac.testing.fakes.FakeMailboxRepository
import dev.jotalac.testing.fakes.FakeMirrorRepository
import dev.jotalac.testing.fakes.FakeGitSyncRepository
import dev.jotalac.testing.fakes.FakeNotebookRepository
import dev.jotalac.testing.fakes.FakeUserSettings
import dev.jotalac.testing.viewModelTest
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.err_create_note_no_notebook
import git_writer.shared.generated.resources.err_failed_create_note
import git_writer.shared.generated.resources.err_paste_image_no_notebook
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalCoroutinesApi::class)
class EditorScreenViewModelTest {

    private val editor = FakeEditorRepository()
    private val notebooks = FakeNotebookRepository()
    private val snackbar = SnackbarManager()
    // auto sync has its own tests - here its endless loop would keep advanceUntilIdle() from ever finishing
    private val settings = FakeUserSettings(UserSettingsState(autoSync = false))
    private val gitSync = FakeGitSyncRepository()
    private val encryptedSync = EncryptedGitSync(gitSync, FakeMirrorRepository(), FakeMailboxRepository())
    // no network, so no request is ever made
    private val remoteStatus = RemoteStatusMonitor({ false }, { flowOf(false) })

    private val notePath = "/notebook/demo.md"
    private val imagePath = "/notebook/picture.png"

    private fun createViewModel(): EditorViewModel =
        EditorViewModel(notebooks, editor, snackbar, settings, gitSync, remoteStatus, encryptedSync)

    /** Subscribes before anything is triggered: the flow does not replay, so a late read would miss it. */
    private fun TestScope.collectSnackbars(): List<SnackbarText> {
        val received = mutableListOf<SnackbarText>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            snackbar.messages.collect { received += it }
        }
        return received
    }

    private fun FakeNotebookRepository.activate(
        path: String,
        directoryPath: String = "/notebook",
    ): Notebook {
        val notebook = Notebook(
            id = 1,
            name = "notebook",
            directoryPath = directoryPath,
            remoteUrl = null,
            remoteUsername = null,
            remotePassword = null,
        )
        activeNotebook.value = notebook
        activeNote.value = path
        return notebook
    }

    // --- remote status ---

    @Test
    fun `the remote is only checked while the editor is watching`() = viewModelTest {
        notebooks.activeNotebook.value = Notebook(1, "notebook", "/notebook", "https://github.com/me/notes.git", "me", "token")
        val checkedUrls = mutableListOf<String>()
        val viewModel = EditorViewModel(
            notebooks, editor, snackbar, settings, gitSync,
            RemoteStatusMonitor({ url -> checkedUrls += url; true }, { flowOf(true) }),
            encryptedSync,
        )
        advanceUntilIdle()

        // the app in the background - nothing collects the status, so nothing goes over the network
        assertTrue(checkedUrls.isEmpty())

        val editorWatching = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.remoteStatus.collect {}
        }
        advanceUntilIdle()

        assertEquals(listOf("https://github.com/me/notes.git"), checkedUrls)
        assertEquals(RemoteStatus.REACHABLE, viewModel.remoteStatus.value)

        // back in the background - the checks stop once the 5 s grace period is over
        editorWatching.cancel()
        advanceUntilIdle()
    }

    // --- loading ---

    @Test
    fun `the active note is loaded into the editor`() = viewModelTest {
        editor.readResult = Result.success("first\n\nsecond")
        editor.existingPaths += notePath
        val viewModel = createViewModel()

        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        assertEquals(listOf(notePath), editor.readPaths)
        assertEquals("first\n\nsecond", viewModel.noteText.value)
    }

    @Test
    fun `a note written on Windows is edited with plain line breaks`() = viewModelTest {
        editor.readResult = Result.success("first\r\nsecond\r\n")
        editor.existingPaths += notePath
        val viewModel = createViewModel()

        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        assertEquals("first\nsecond\n", viewModel.noteText.value)
    }

    @Test
    fun `an image note is marked as an image instead of being read as markdown`() = viewModelTest {
        editor.existingPaths += imagePath
        val viewModel = createViewModel()

        notebooks.activeNote.value = imagePath
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isImage)
        assertTrue(editor.readPaths.isEmpty())
    }

    @Test
    fun `switching the active note loads the new file`() = viewModelTest {
        editor.existingPaths += listOf("/notebook/first.md", "/notebook/second.md")
        createViewModel()

        notebooks.activeNote.value = "/notebook/first.md"
        advanceUntilIdle()
        notebooks.activeNote.value = "/notebook/second.md"
        advanceUntilIdle()

        assertEquals(listOf("/notebook/first.md", "/notebook/second.md"), editor.readPaths)
    }

    @Test
    fun `a missing note reports an error instead of loading`() = viewModelTest {
        val viewModel = createViewModel()

        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        assertEquals("Error loading file - $notePath", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(editor.readPaths.isEmpty())
    }

    @Test
    fun `a failed load leaves the editor empty and never saves over the note`() = viewModelTest {
        editor.existingPaths += notePath
        editor.readResult = Result.failure(IllegalStateException("unreadable"))
        val viewModel = createViewModel()

        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        assertEquals("", viewModel.noteText.value)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Error loading file - $notePath", viewModel.uiState.value.error)

        viewModel.onAction(EditorAction.UpdateText("typed"))
        advanceTimeBy(5_000.milliseconds)
        advanceUntilIdle()
        assertTrue(editor.savedFiles.isEmpty())
    }

    // --- autosave ---

    @Test
    fun `autosave waits for the debounce and then saves the text`() = viewModelTest {
        editor.existingPaths += notePath
        editor.readResult = Result.success("first\n\nsecond")
        val viewModel = createViewModel()
        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        viewModel.onAction(EditorAction.UpdateText("changed\n\nsecond"))

        advanceTimeBy(500.milliseconds)
        assertTrue(editor.savedFiles.isEmpty(), "saved before the debounce elapsed")

        advanceTimeBy(1_000.milliseconds)
        advanceUntilIdle()

        assertEquals(1, editor.savedFiles.size)
        assertEquals(notePath, editor.savedFiles.single().path)
        assertEquals("changed\n\nsecond", editor.savedFiles.single().content)
    }

    @Test
    fun `an image note is never autosaved`() = viewModelTest {
        editor.existingPaths += imagePath
        val viewModel = createViewModel()
        notebooks.activeNote.value = imagePath
        advanceUntilIdle()

        viewModel.onAction(EditorAction.UpdateText("changed"))
        advanceTimeBy(5_000.milliseconds)
        advanceUntilIdle()

        assertTrue(editor.savedFiles.isEmpty())
    }

    @Test
    fun `editing is not saved before a note is open`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onAction(EditorAction.UpdateText("changed"))
        advanceTimeBy(5_000.milliseconds)
        advanceUntilIdle()

        assertTrue(editor.savedFiles.isEmpty())
    }

    // --- creating notes ---

    @Test
    fun `creating a note without a notebook asks for one`() = viewModelTest {
        val viewModel = createViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(EditorAction.NewNote)
        advanceUntilIdle()

        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.err_create_note_no_notebook)), messages)
    }

    @Test
    fun `a failing note creation reports the failure with its cause`() = viewModelTest {
        notebooks.activeNotebook.value = Notebook(
            id = 1,
            name = "notebook",
            directoryPath = "/notebook",
            remoteUrl = null,
            remoteUsername = null,
            remotePassword = null,
        )
        editor.createNoteResult = Result.failure(IllegalStateException("disk full"))
        val viewModel = createViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(EditorAction.NewNote)
        advanceUntilIdle()

        assertEquals(
            listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_create_note, "disk full")),
            messages,
        )
    }

    @Test
    fun `creating a note opens the file it created`() = viewModelTest {
        notebooks.activeNotebook.value = Notebook(
            id = 1,
            name = "notebook",
            directoryPath = "/notebook",
            remoteUrl = null,
            remoteUsername = null,
            remotePassword = null,
        )
        editor.createNoteResult = Result.success("/notebook/untitled.md")
        editor.existingPaths += "/notebook/untitled.md"
        editor.readResult = Result.success("created")
        val viewModel = createViewModel()

        viewModel.onAction(EditorAction.NewNote)
        advanceUntilIdle()

        assertEquals("/notebook", editor.createdNotes.single().directoryPath)
        assertEquals(listOf("/notebook/untitled.md"), notebooks.activatedNotes)
        assertEquals(listOf("/notebook/untitled.md"), editor.readPaths)
    }

    // --- pasting ---

    @Test
    fun `pasting an image without a notebook asks for one`() = viewModelTest {
        val viewModel = createViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(
            EditorAction.PasteImages(
                imageBytesList = listOf(byteArrayOf(1, 2, 3)),
                onImagesSaved = { error("nothing was saved") },
            )
        )
        advanceUntilIdle()

        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.err_paste_image_no_notebook)), messages)
        assertTrue(editor.pastedImages.isEmpty())
    }

    @Test
    fun `pasted images are saved in the notebook and handed back as markdown`() = viewModelTest {
        notebooks.activeNotebook.value = Notebook(
            id = 1,
            name = "notebook",
            directoryPath = "/notebook",
            remoteUrl = null,
            remoteUsername = null,
            remotePassword = null,
        )
        val viewModel = createViewModel()
        var imageMarkdown = emptyList<String>()

        viewModel.onAction(
            EditorAction.PasteImages(
                imageBytesList = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5)),
                onImagesSaved = { imageMarkdown = it },
            )
        )
        advanceUntilIdle()

        assertEquals(2, editor.pastedImages.size)
        assertEquals(
            editor.pastedImages.map { "![pasted image](images/${it.filename})" },
            imageMarkdown,
        )
    }

    // --- tabs ---

    @Test
    fun `a new tab becomes the active one`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onAction(EditorAction.NewTab)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.openedTabs.size)
        assertEquals(state.openedTabs.last().id, state.activeTabId)
    }

    @Test
    fun `next tab wraps around to the first`() = viewModelTest {
        val viewModel = createViewModel()
        viewModel.onAction(EditorAction.NewTab)
        advanceUntilIdle()
        val firstTabId = viewModel.uiState.value.openedTabs.first().id

        viewModel.onAction(EditorAction.NextTab)
        advanceUntilIdle()

        assertEquals(firstTabId, viewModel.uiState.value.activeTabId)
    }

    @Test
    fun `a note is left alone when it is not the active one`() = viewModelTest {
        editor.existingPaths += notePath
        val viewModel = createViewModel()
        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        viewModel.openTab(id = 999)

        assertEquals(0L, viewModel.uiState.value.activeTabId)
        assertNull(viewModel.uiState.value.error)
    }
}
