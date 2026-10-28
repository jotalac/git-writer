package dev.jotalac.feature.editor_sidebar.ui

import androidx.lifecycle.viewModelScope
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.core.utils.deleteRecursively
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.pdf_export.PdfExportRequest
import dev.jotalac.testing.fakes.FakeEditorRepository
import dev.jotalac.testing.fakes.FakeGitSyncRepository
import dev.jotalac.testing.fakes.FakeNotebookRepository
import dev.jotalac.testing.viewModelTest
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.err_failed_add_note
import git_writer.shared.generated.resources.err_failed_delete
import git_writer.shared.generated.resources.err_failed_export_pdf
import git_writer.shared.generated.resources.err_failed_move
import git_writer.shared.generated.resources.err_failed_rename
import git_writer.shared.generated.resources.msg_pdf_exported
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.writeString
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
* The sidebar reads the file tree from disk, so every test gets a real temporary notebook folder.
* The repositories are fakes: they record what the sidebar asked for and can be told to fail.
* */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorSidebarViewModelTest {

    private val editor = FakeEditorRepository()
    private val notebooks = FakeNotebookRepository()
    private val snackbar = SnackbarManager()
    private val gitSync = FakeGitSyncRepository()

    private lateinit var notebookDir: Path

    @BeforeTest
    fun createNotebookFolder() {
        notebookDir = Path(SystemTemporaryDirectory, "git_writer_sidebar_test_${Random.nextInt()}")
        SystemFileSystem.createDirectories(notebookDir)
    }

    @AfterTest
    fun deleteNotebookFolder() {
        notebookDir.deleteRecursively()
    }

    private val viewModels = mutableListOf<EditorSidebarViewModel>()

    private fun createViewModel() = EditorSidebarViewModel(notebooks, editor, snackbar, gitSync).also { viewModels += it }

    /**
     * [viewModelTest] that also stops the ViewModels before the test ends. Their tree refresh runs on the
     * IO dispatcher - finishing after Dispatchers.Main is reset, it would fail whichever test runs next.
     */
    private fun sidebarTest(block: suspend TestScope.() -> Unit) = viewModelTest {
        try {
            block()
        } finally {
            viewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        }
    }

    private fun pathOf(relativePath: String): String = Path(notebookDir, relativePath).toString()

    private fun file(relativePath: String): String {
        val path = Path(notebookDir, relativePath)
        path.parent?.let { SystemFileSystem.createDirectories(it) }
        SystemFileSystem.sink(path).buffered().use { it.writeString("content") }
        return path.toString()
    }

    private fun folder(relativePath: String): String {
        val path = Path(notebookDir, relativePath)
        SystemFileSystem.createDirectories(path)
        return path.toString()
    }

    private fun openNotebook(directory: String = notebookDir.toString()) {
        notebooks.activeNotebook.value = Notebook(
            id = directory.hashCode().toLong(),
            name = "notes",
            directoryPath = directory,
            remoteUrl = null,
            remoteUsername = null,
            remotePassword = null,
        )
    }

    /** The tree is read on the IO dispatcher, so tests wait for it instead of advancing time. */
    private suspend fun EditorSidebarViewModel.awaitTree(
        condition: (FileNode.Directory) -> Boolean = { true },
    ): FileNode.Directory = uiState.mapNotNull { it.fileTree }.first(condition)

    private fun FileNode.Directory.containsPath(path: String): Boolean =
        children.any { it.path == path || (it is FileNode.Directory && it.containsPath(path)) }

    /** Subscribes before anything is triggered: the flow does not replay, so a late read would miss it. */
    private fun TestScope.collectSnackbars(): List<SnackbarText> {
        val received = mutableListOf<SnackbarText>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            snackbar.messages.collect { received += it }
        }
        return received
    }

    // --- file tree

    @Test
    fun `the tree lists folders first and only notes and images`() = sidebarTest {
        file("b.md")
        file("a.md")
        file("photo.png")
        file("zfolder/inside.md")
        folder("afolder")
        file("todo.txt")
        file(".hidden.md")
        file(".git/config")

        val tree = createViewModel().apply { openNotebook() }.awaitTree()

        assertEquals(listOf("afolder", "zfolder", "a.md", "b.md", "photo.png"), tree.children.map { it.name })
    }

    @Test
    fun `closing the notebook clears the tree`() = sidebarTest {
        file("a.md")
        val viewModel = createViewModel()
        openNotebook()
        viewModel.awaitTree()

        notebooks.activeNotebook.value = null
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.fileTree)
    }

    @Test
    fun `opening another notebook collapses the folders of the previous one`() = sidebarTest {
        val docs = folder("docs")
        val viewModel = createViewModel()
        openNotebook()
        viewModel.awaitTree()
        viewModel.toggleFolder(docs)

        openNotebook(directory = folder("other-notebook"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.expandedFolders.isEmpty())
    }

    // --- what is visible in the tree

    private val note = FileNode.File("note.md", "/nb/docs/note.md")
    private val draft = FileNode.File("draft.md", "/nb/docs/drafts/draft.md")
    private val drafts = FileNode.Directory("drafts", "/nb/docs/drafts", listOf(draft))
    private val docs = FileNode.Directory("docs", "/nb/docs", listOf(drafts, note))
    private val root = FileNode.Directory("nb", "/nb", listOf(docs))

    @Test
    fun `children of a collapsed folder are hidden`() = sidebarTest {
        val visible = createViewModel().getVisibleNodes(root, expandedPaths = emptySet())

        assertEquals(listOf(FlatFileNode(docs, depth = 0, isExpanded = false)), visible)
    }

    @Test
    fun `an expanded folder shows its children one level deeper`() = sidebarTest {
        // drafts stays collapsed, so its note is not shown even though docs is open
        val visible = createViewModel().getVisibleNodes(root, expandedPaths = setOf("/nb/docs"))

        assertEquals(
            listOf(
                FlatFileNode(docs, depth = 0, isExpanded = true),
                FlatFileNode(drafts, depth = 1, isExpanded = false),
                FlatFileNode(note, depth = 1),
            ),
            visible,
        )
    }

    @Test
    fun `expand all opens every nested folder and the next click collapses them all`() = sidebarTest {
        val docs = folder("docs")
        val drafts = folder("docs/drafts")
        val other = folder("other")
        val viewModel = createViewModel()
        openNotebook()
        viewModel.awaitTree()

        viewModel.toggleFolderCollapse()
        assertTrue(viewModel.uiState.value.expandedFolders.containsAll(listOf(docs, drafts, other)))

        viewModel.toggleFolderCollapse()
        assertTrue(viewModel.uiState.value.expandedFolders.isEmpty())
    }

    // --- renaming

    private suspend fun TestScope.loadedViewModel(): EditorSidebarViewModel {
        val viewModel = createViewModel()
        openNotebook()
        viewModel.awaitTree()
        return viewModel
    }

    @Test
    fun `renaming a note keeps its extension`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.RenameItem(newName = "ideas", path = note))
        advanceUntilIdle()

        assertEquals(listOf(note to "ideas.md"), editor.renamedItems)
    }

    @Test
    fun `typing the extension does not add it twice`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.RenameItem(newName = "ideas.md", path = note))
        advanceUntilIdle()

        assertEquals(listOf(note to "ideas.md"), editor.renamedItems)
    }

    @Test
    fun `characters a file name cannot contain are replaced`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.RenameItem(newName = "q1/q2: plan?", path = note))
        advanceUntilIdle()

        assertEquals(listOf(note to "q1_q2_ plan_.md"), editor.renamedItems)
    }

    @Test
    fun `a folder with a dot in its name does not get an extension`() = sidebarTest {
        val version = folder("v1.2")
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.RenameItem(newName = "v2", path = version))
        advanceUntilIdle()

        assertEquals(listOf(version to "v2"), editor.renamedItems)
    }

    @Test
    fun `an empty name cancels renaming without touching the file`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()
        viewModel.onAction(SidebarAction.SetRenameItem(note))

        viewModel.onAction(SidebarAction.RenameItem(newName = "   ", path = note))
        advanceUntilIdle()

        assertTrue(editor.renamedItems.isEmpty())
        assertNull(viewModel.uiState.value.itemToRename)
    }

    @Test
    fun `keeping the same name does not touch the file`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.RenameItem(newName = "note", path = note))
        advanceUntilIdle()

        assertTrue(editor.renamedItems.isEmpty())
    }

    @Test
    fun `renaming the open note keeps it open under the new name`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()
        notebooks.activeNote.value = note
        advanceUntilIdle()

        viewModel.onAction(SidebarAction.RenameItem(newName = "ideas", path = note))
        advanceUntilIdle()

        assertEquals(pathOf("ideas.md"), notebooks.activeNote.value)
    }

    @Test
    fun `a failed rename is reported with its cause`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()
        editor.renameResult = Result.failure(IllegalStateException("name already taken"))
        val messages = collectSnackbars()

        viewModel.onAction(SidebarAction.RenameItem(newName = "ideas", path = note))
        advanceUntilIdle()

        assertEquals(
            listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_rename, "name already taken")),
            messages,
        )
    }

    // --- creating

    @Test
    fun `a new note in a folder is opened with its folder expanded and its name ready to edit`() = sidebarTest {
        val docs = folder("docs")
        val newNote = pathOf("docs/untitled.md")
        editor.createNoteResult = Result.success(newNote)
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.AddNote(path = docs))
        advanceUntilIdle()

        assertEquals(docs, editor.createdNotes.single().directoryPath)
        assertTrue(docs in viewModel.uiState.value.expandedFolders)
        assertEquals(newNote, notebooks.activeNote.value)
        assertEquals(newNote, viewModel.uiState.value.itemToRename)
    }

    @Test
    fun `a new folder gets the first name that is not taken`() = sidebarTest {
        folder("untitled")
        folder("untitled 1")
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.AddFolder())
        advanceUntilIdle()

        assertEquals(listOf(notebookDir.toString() to "untitled 2"), editor.addedFolders)
        assertEquals(pathOf("untitled 2"), viewModel.uiState.value.itemToRename)
    }

    @Test
    fun `duplicating a note copies its content next to it`() = sidebarTest {
        val shopping = file("docs/shopping.md")
        editor.readResult = Result.success("- milk\n- bread")
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.DuplicateNote(shopping))
        advanceUntilIdle()

        assertEquals(
            listOf(FakeEditorRepository.CreatedNote(pathOf("docs"), "shopping", "- milk\n- bread")),
            editor.createdNotes,
        )
    }

    @Test
    fun `a note that cannot be read is not duplicated`() = sidebarTest {
        val shopping = file("shopping.md")
        editor.readResult = Result.failure(IllegalStateException("permission denied"))
        val viewModel = loadedViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(SidebarAction.DuplicateNote(shopping))
        advanceUntilIdle()

        assertTrue(editor.createdNotes.isEmpty())
        assertEquals(
            listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_add_note, "permission denied")),
            messages,
        )
    }

    // --- PDF export

    @Test
    fun `exporting a note hands its name and content to the export`() = sidebarTest {
        val shopping = file("docs/shopping.md")
        editor.readResult = Result.success("- milk\n- bread")
        val viewModel = loadedViewModel()

        viewModel.onAction(SidebarAction.ExportPdf(shopping))
        advanceUntilIdle()

        assertEquals(PdfExportRequest("shopping", "- milk\n- bread"), viewModel.uiState.value.pdfExport)
    }

    @Test
    fun `another export while one is running is ignored`() = sidebarTest {
        val viewModel = exportingViewModel()
        val other = file("other.md")
        editor.readResult = Result.success("other content")

        viewModel.onAction(SidebarAction.ExportPdf(other))
        advanceUntilIdle()

        assertEquals("shopping", viewModel.uiState.value.pdfExport?.noteName)
    }

    @Test
    fun `a finished export ends and says what the PDF was saved as`() = sidebarTest {
        val viewModel = exportingViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(SidebarAction.PdfExportFinished(Result.success("shopping.pdf")))

        assertNull(viewModel.uiState.value.pdfExport)
        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.msg_pdf_exported, "shopping.pdf")), messages)
    }

    @Test
    fun `a cancelled save dialog ends the export without a message`() = sidebarTest {
        val viewModel = exportingViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(SidebarAction.PdfExportFinished(Result.success(null)))

        assertNull(viewModel.uiState.value.pdfExport)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `a failed export ends and shows why`() = sidebarTest {
        val viewModel = exportingViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(SidebarAction.PdfExportFinished(Result.failure(IllegalStateException("disk full"))))

        assertNull(viewModel.uiState.value.pdfExport)
        assertEquals(listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_export_pdf, "disk full")), messages)
    }

    @Test
    fun `a note that cannot be read is not exported`() = sidebarTest {
        val shopping = file("shopping.md")
        editor.readResult = Result.failure(IllegalStateException("permission denied"))
        val viewModel = loadedViewModel()
        val messages = collectSnackbars()

        viewModel.onAction(SidebarAction.ExportPdf(shopping))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pdfExport)
        assertEquals(listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_export_pdf, "permission denied")), messages)
    }

    /** A ViewModel in the middle of exporting a note. */
    private suspend fun TestScope.exportingViewModel(): EditorSidebarViewModel {
        val shopping = file("shopping.md")
        val viewModel = loadedViewModel()
        viewModel.onAction(SidebarAction.ExportPdf(shopping))
        advanceUntilIdle()
        return viewModel
    }

    // --- moving and deleting

    @Test
    fun `moving the open note keeps it open at its new place`() = sidebarTest {
        val note = file("note.md")
        val archive = folder("archive")
        val viewModel = loadedViewModel()
        notebooks.activeNote.value = note
        advanceUntilIdle()

        viewModel.onAction(SidebarAction.MoveItem(sourcePath = note, destinationDirectoryPath = archive))
        advanceUntilIdle()

        assertEquals(pathOf("archive/note.md"), notebooks.activeNote.value)
    }

    @Test
    fun `a failed move keeps the note where it was and reports the cause`() = sidebarTest {
        val note = file("note.md")
        val archive = folder("archive")
        val viewModel = loadedViewModel()
        notebooks.activeNote.value = note
        advanceUntilIdle()
        editor.moveResult = Result.failure(IllegalStateException("already exists"))
        val messages = collectSnackbars()

        viewModel.onAction(SidebarAction.MoveItem(sourcePath = note, destinationDirectoryPath = archive))
        advanceUntilIdle()

        assertEquals(note, notebooks.activeNote.value)
        assertEquals(listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_move, "already exists")), messages)
    }

    @Test
    fun `deleting the open note closes it`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()
        notebooks.activeNote.value = note
        advanceUntilIdle()

        viewModel.onAction(SidebarAction.DeleteItem(note))
        advanceUntilIdle()

        assertNull(notebooks.activeNote.value)
    }

    @Test
    fun `a failed delete keeps the note open and reports the cause`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()
        notebooks.activeNote.value = note
        advanceUntilIdle()
        editor.deleteResult = Result.failure(IllegalStateException("file is locked"))
        val messages = collectSnackbars()

        viewModel.onAction(SidebarAction.DeleteItem(note))
        advanceUntilIdle()

        assertEquals(note, notebooks.activeNote.value)
        assertEquals(listOf<SnackbarText>(SnackbarText.message(Res.string.err_failed_delete, "file is locked")), messages)
    }

    // --- notes created outside of the sidebar (Ctrl+N)

    @Test
    fun `a note created outside the sidebar is added to the tree and its name made editable`() = sidebarTest {
        val viewModel = loadedViewModel()

        val newNote = file("from-shortcut.md")
        notebooks.activeNote.value = newNote

        viewModel.awaitTree { it.containsPath(newNote) }
        assertEquals(newNote, viewModel.uiState.value.itemToRename)
    }

    @Test
    fun `opening a note that is already in the tree does not start renaming it`() = sidebarTest {
        val note = file("note.md")
        val viewModel = loadedViewModel()

        notebooks.activeNote.value = note
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.itemToRename)
    }

    @Test
    fun `renaming another note does not stop the next new note from being renamed`() = sidebarTest {
        val open = file("open.md")
        val other = file("other.md")
        val viewModel = loadedViewModel()
        notebooks.activeNote.value = open
        advanceUntilIdle()

        // the fake only records the rename, so do it on disk like the real repository - waiting for the
        // tree to show it makes sure the reload started by the rename is done before the new note exists
        SystemFileSystem.atomicMove(Path(other), Path(pathOf("renamed.md")))
        viewModel.onAction(SidebarAction.RenameItem(newName = "renamed", path = other))
        viewModel.awaitTree { it.containsPath(pathOf("renamed.md")) }

        val newNote = file("from-shortcut.md")
        notebooks.activeNote.value = newNote
        advanceUntilIdle()

        assertEquals(newNote, viewModel.uiState.value.itemToRename)
    }

    // --- reloading the tree

    @Test
    fun `coming back to the app shows files changed outside of it`() = sidebarTest {
        val viewModel = loadedViewModel()

        val pulled = file("changed-outside.md")
        viewModel.onWindowFocusChanged(hasFocus = true)

        viewModel.awaitTree { it.containsPath(pulled) }
    }

    @Test
    fun `a finished git sync shows the pulled files`() = sidebarTest {
        val viewModel = loadedViewModel()

        val pulled = file("pulled.md")
        gitSync.status.emit(GitSyncStatus.UpToDate)

        viewModel.awaitTree { it.containsPath(pulled) }
    }
}
