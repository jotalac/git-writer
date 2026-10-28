package dev.jotalac.feature.notebooks_management.ui.create_notebook

import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import dev.jotalac.feature.notebooks_management.ui.create_notebook.CreateNotebookViewModel.CreateNotebookEvent
import dev.jotalac.testing.fakes.FakeNotebookRepository
import dev.jotalac.testing.fakes.FakeNotebookRepository.ClonedNotebook
import dev.jotalac.testing.viewModelTest
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.err_failed_create_base_directory
import git_writer.shared.generated.resources.msg_notebook_created
import git_writer.shared.generated.resources.msg_notebook_opened
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateNotebookViewModelTest {

    private val notebooks = FakeNotebookRepository()
    private val snackbar = SnackbarManager()

    private val work = Notebook(7, "Work", "/notebooks/Work", null, null, null)
    private var dialogClosed = false

    private fun createViewModel() = CreateNotebookViewModel(
        notebookRepository = notebooks,
        snackbarManager = snackbar,
        notebookPathProvider = object : NotebookPathProvider {
            override fun getDefaultNotebookDirectory() = "/notebooks"
        },
    )

    private fun TestScope.collectSnackbars(): List<SnackbarText> {
        val received = mutableListOf<SnackbarText>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            snackbar.messages.collect { received += it }
        }
        return received
    }

    @Test
    fun `opening the dialog makes sure the notebooks folder exists`() = viewModelTest {
        val viewModel = createViewModel()

        assertEquals(listOf("/notebooks"), notebooks.createdBaseDirectories)
        assertEquals("/notebooks", viewModel.uiState.value.defaultBasePath)
    }

    @Test
    fun `a notebooks folder that cannot be created is reported`() = viewModelTest {
        notebooks.baseDirectoryResult = Result.failure(IllegalStateException("read-only file system"))
        val messages = collectSnackbars()

        createViewModel()

        assertEquals(
            listOf<SnackbarText>(SnackbarText.resource(Res.string.err_failed_create_base_directory)),
            messages,
        )
    }

    @Test
    fun `a created notebook is opened and the form is cleared and the dialog closes`() = viewModelTest {
        notebooks.notebookResult = Result.success(work)
        val viewModel = createViewModel()
        val messages = collectSnackbars()
        viewModel.onEvent(CreateNotebookEvent.NotebookNameChanged("Work"))

        viewModel.onEvent(CreateNotebookEvent.CreateLocalNotebook("/notebooks/Work") { dialogClosed = true })
        advanceUntilIdle()

        assertEquals(listOf(work.id), notebooks.activatedNotebookIds)
        assertEquals("", viewModel.uiState.value.notebookName)
        assertTrue(dialogClosed)
        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.msg_notebook_created)), messages)
    }

    @Test
    fun `a failed create keeps what was typed and shows why`() = viewModelTest {
        notebooks.notebookResult = Result.failure(IllegalStateException("Directory already exists"))
        val viewModel = createViewModel()
        viewModel.onEvent(CreateNotebookEvent.NotebookNameChanged("Work"))

        viewModel.onEvent(CreateNotebookEvent.CreateLocalNotebook("/notebooks/Work") { dialogClosed = true })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Directory already exists", state.errorMessage)
        assertEquals("Work", state.notebookName)
        assertFalse(state.isLoading)
        assertFalse(dialogClosed)
        assertTrue(notebooks.activatedNotebookIds.isEmpty())
    }

    @Test
    fun `a notebook that was added but cannot be opened keeps the dialog open with the reason`() = viewModelTest {
        notebooks.notebookResult = Result.success(work)
        notebooks.activateResult = Result.failure(IllegalStateException("folder not readable"))
        val viewModel = createViewModel()
        val messages = collectSnackbars()
        viewModel.onEvent(CreateNotebookEvent.NotebookNameChanged("Work"))

        viewModel.onEvent(CreateNotebookEvent.CreateLocalNotebook("/notebooks/Work") { dialogClosed = true })
        advanceUntilIdle()

        assertFalse(dialogClosed)
        assertContains(viewModel.uiState.value.errorMessage.orEmpty(), "folder not readable")
        // the notebook exists - submitting the same form again would only fail
        assertEquals("", viewModel.uiState.value.notebookName)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `cloning uses the remote and credentials from the form`() = viewModelTest {
        notebooks.notebookResult = Result.success(work)
        val viewModel = createViewModel()
        viewModel.onEvent(CreateNotebookEvent.NotebookNameChanged("Team"))
        // pasted urls often come with spaces around them
        viewModel.onEvent(CreateNotebookEvent.RemoteUrlChanged("  https://example.com/team.git "))
        viewModel.onEvent(CreateNotebookEvent.UsernameChanged("me"))
        viewModel.onEvent(CreateNotebookEvent.PasswordChanged("token"))

        viewModel.onEvent(CreateNotebookEvent.CloneRemoteNotebook("/notebooks/Team") { dialogClosed = true })
        advanceUntilIdle()

        assertEquals(
            listOf(ClonedNotebook("Team", "/notebooks/Team", "https://example.com/team.git", "token", "me")),
            notebooks.clonedNotebooks,
        )
        assertTrue(dialogClosed)
    }

    @Test
    fun `cloning an encrypted notebook passes its password`() = viewModelTest {
        notebooks.notebookResult = Result.success(work)
        val viewModel = createViewModel()
        viewModel.onEvent(CreateNotebookEvent.NotebookNameChanged("Diary"))
        viewModel.onEvent(CreateNotebookEvent.RemoteUrlChanged("https://example.com/diary.git"))
        viewModel.onEvent(CreateNotebookEvent.UsernameChanged("me"))
        viewModel.onEvent(CreateNotebookEvent.PasswordChanged("token"))
        viewModel.onEvent(CreateNotebookEvent.EncryptionPasswordChanged("correct horse"))

        viewModel.onEvent(CreateNotebookEvent.CloneRemoteNotebook("/notebooks/Diary") { dialogClosed = true })
        advanceUntilIdle()

        assertEquals("correct horse", notebooks.clonedNotebooks.single().encryptionPassword)
    }

    @Test
    fun `opening an existing folder says it was opened and not created`() = viewModelTest {
        notebooks.notebookResult = Result.success(work)
        val viewModel = createViewModel()
        val messages = collectSnackbars()
        viewModel.onEvent(CreateNotebookEvent.DirectorySelected("/Users/me/Diary"))

        viewModel.onEvent(CreateNotebookEvent.OpenExistingNotebook { dialogClosed = true })
        advanceUntilIdle()

        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.msg_notebook_opened)), messages)
        assertTrue(dialogClosed)
    }

    @Test
    fun `opening an existing notebook without choosing a folder does nothing`() = viewModelTest {
        notebooks.notebookResult = Result.success(work)
        val viewModel = createViewModel()

        viewModel.onEvent(CreateNotebookEvent.OpenExistingNotebook { dialogClosed = true })
        advanceUntilIdle()

        assertTrue(notebooks.activatedNotebookIds.isEmpty())
        assertFalse(dialogClosed)
    }
}
