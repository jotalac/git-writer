package dev.jotalac.feature.notebooks_management.ui.list_notebooks

import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import dev.jotalac.feature.notebooks_management.ui.list_notebooks.NotebookListViewModel.NotebookListEvent
import dev.jotalac.testing.fakes.FakeGitSyncRepository
import dev.jotalac.testing.fakes.FakeNotebookRepository
import dev.jotalac.testing.viewModelTest
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.msg_notebook_deleted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotebookListViewModelTest {

    private val notebooks = FakeNotebookRepository()
    private val gitSync = FakeGitSyncRepository()
    private val snackbar = SnackbarManager()

    private val team = Notebook(7, "Team", "/nb/Team", "https://example.com/team.git", "me", "token")
    private var dialogClosed = false

    private fun createViewModel() = NotebookListViewModel(notebooks, gitSync, snackbar, object : NotebookPathProvider {
        override fun getDefaultNotebookDirectory() = "/nb"
    })

    private fun TestScope.collectSnackbars(): List<SnackbarText> {
        val received = mutableListOf<SnackbarText>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            snackbar.messages.collect { received += it }
        }
        return received
    }

    @Test
    fun `opening a notebook closes the list and shows its sync state`() = viewModelTest {
        notebooks.notebookResult = Result.success(team)
        val viewModel = createViewModel()

        viewModel.onEvent(NotebookListEvent.OpenNotebook(team.id) { dialogClosed = true })
        advanceUntilIdle()

        assertEquals(listOf(team.id), notebooks.activatedNotebookIds)
        assertTrue(dialogClosed)
        // a remote url means syncing is set up for it
        assertEquals(team.remoteUrl, gitSync.syncStatusRemoteUrls.last())
    }

    @Test
    fun `a notebook that cannot be opened shows why and keeps the list open`() = viewModelTest {
        notebooks.notebookResult = Result.failure(IllegalStateException("Notebook directory doesn't exist anymore"))
        val viewModel = createViewModel()

        viewModel.onEvent(NotebookListEvent.OpenNotebook(team.id) { dialogClosed = true })
        advanceUntilIdle()

        assertEquals("Notebook directory doesn't exist anymore", viewModel.uiState.value.errorMessage)
        assertFalse(dialogClosed)
    }

    @Test
    fun `deleting a notebook confirms it`() = viewModelTest {
        val viewModel = createViewModel()
        val messages = collectSnackbars()

        viewModel.onEvent(NotebookListEvent.DeleteNotebook(team.id))
        advanceUntilIdle()

        assertEquals(listOf(team.id), notebooks.deletedNotebookIds)
        assertEquals(listOf<SnackbarText>(SnackbarText.resource(Res.string.msg_notebook_deleted)), messages)
    }

    @Test
    fun `a failed delete shows why and confirms nothing`() = viewModelTest {
        notebooks.deleteResult = Result.failure(IllegalStateException("folder is in use"))
        val viewModel = createViewModel()
        val messages = collectSnackbars()

        viewModel.onEvent(NotebookListEvent.DeleteNotebook(team.id))
        advanceUntilIdle()

        assertEquals("folder is in use", viewModel.uiState.value.errorMessage)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `a list that cannot be loaded shows an error instead of crashing`() = viewModelTest {
        notebooks.notebooksError = IllegalStateException("database is corrupted")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}
