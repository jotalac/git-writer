package dev.jotalac.feature.editor.ui

import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.testing.fakes.FakeEditorRepository
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
    private val settings = FakeUserSettings()
    private val gitSync = FakeGitSyncRepository()

    private val notePath = "/notebook/demo.md"
    private val imagePath = "/notebook/picture.png"

    private fun createViewModel(): EditorViewModel =
        EditorViewModel(notebooks, editor, snackbar, settings, gitSync)

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

    // --- loading ---

    @Test
    fun `the active note is loaded into the editor`() = viewModelTest {
        editor.blocksResult = Result.success(listOf("first", "second"))
        editor.existingPaths += notePath
        createViewModel()

        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        assertEquals(listOf(notePath), editor.loadedPaths)
    }

    @Test
    fun `an image note is marked as an image instead of being read as markdown`() = viewModelTest {
        editor.existingPaths += imagePath
        val viewModel = createViewModel()

        notebooks.activeNote.value = imagePath
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isImage)
        assertTrue(editor.loadedPaths.isEmpty())
    }

    @Test
    fun `switching the active note loads the new file`() = viewModelTest {
        editor.existingPaths += listOf("/notebook/first.md", "/notebook/second.md")
        createViewModel()

        notebooks.activeNote.value = "/notebook/first.md"
        advanceUntilIdle()
        notebooks.activeNote.value = "/notebook/second.md"
        advanceUntilIdle()

        assertEquals(listOf("/notebook/first.md", "/notebook/second.md"), editor.loadedPaths)
    }

    @Test
    fun `a missing note reports an error instead of loading`() = viewModelTest {
        val viewModel = createViewModel()

        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        assertEquals("Error loading file - $notePath", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(editor.loadedPaths.isEmpty())
    }

    @Test
    fun `a failed load leaves the editor empty rather than half loaded`() = viewModelTest {
        editor.existingPaths += notePath
        editor.blocksResult = Result.failure(IllegalStateException("unreadable"))
        val viewModel = createViewModel()

        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        assertTrue(viewModel.markdownBlocks.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // --- autosave ---

    @Test
    fun `autosave waits for the debounce and then saves the blocks`() = viewModelTest {
        editor.existingPaths += notePath
        editor.blocksResult = Result.success(listOf("first", "second"))
        val viewModel = createViewModel()
        notebooks.activeNote.value = notePath
        advanceUntilIdle()

        viewModel.onAction(EditorAction.UpdateBlock(index = 0, newText = "changed"))

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

        viewModel.onAction(EditorAction.UpdateBlock(index = 0, newText = "changed"))
        advanceTimeBy(5_000.milliseconds)
        advanceUntilIdle()

        assertTrue(editor.savedFiles.isEmpty())
    }

    @Test
    fun `editing is not saved before a note is open`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onAction(EditorAction.UpdateBlock(index = 0, newText = "changed"))
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
        editor.blocksResult = Result.success(listOf("created"))
        val viewModel = createViewModel()

        viewModel.onAction(EditorAction.NewNote)
        advanceUntilIdle()

        assertEquals("/notebook", editor.createdNotes.single().directoryPath)
        assertEquals(listOf("/notebook/untitled.md"), notebooks.activatedNotes)
        assertEquals(listOf("/notebook/untitled.md"), editor.loadedPaths)
    }

    // --- pasting ---

    @Test
    fun `pasting an image without a notebook asks for one`() = viewModelTest {
        val viewModel = createViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(
            EditorAction.PasteImages(
                imageBytesList = listOf(byteArrayOf(1, 2, 3)),
                focusedIndex = 0,
                onFocusCalculated = {},
            )
        )
        advanceUntilIdle()

        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.err_paste_image_no_notebook)), messages)
        assertTrue(editor.pastedImages.isEmpty())
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
