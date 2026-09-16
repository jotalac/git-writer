package dev.jotalac.feature.notebooks_management.ui.create_notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.err_failed_create_base_directory
import git_writer.shared.generated.resources.msg_notebook_created
import git_writer.shared.generated.resources.msg_notebook_opened

data class CreateNotebookState(
    val selectedTabIndex: Int = 0,
    val notebookName: String = "",
    val remoteUrl: String = "",
    val username: String = "",
    val password: String = "",
    val defaultBasePath: String = "",
    val selectedDirectory: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
)

class CreateNotebookViewModel(
    private val notebookRepository: NotebookRepository,
    private val snackbarManager: SnackbarManager,
    notebookPathProvider: NotebookPathProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateNotebookState())
    val uiState: StateFlow<CreateNotebookState> = _uiState.asStateFlow()

    init {
        val basePath = notebookPathProvider.getDefaultNotebookDirectory()
        _uiState.update { currentState ->
            currentState.copy(
                defaultBasePath = basePath
            )
        }

        // make sure the notebooks root directory exists
        notebookRepository.createBaseNotebooksDirectory(basePath).onFailure {
            snackbarManager.showMessage(SnackbarText.resource(Res.string.err_failed_create_base_directory))
        }

    }

    fun onEvent(event: CreateNotebookEvent) {
        when (event) {
            is CreateNotebookEvent.TabSelected -> _uiState.update {
                it.copy(
                    selectedTabIndex = event.index,
                    errorMessage = null
                )
            }

            is CreateNotebookEvent.NotebookNameChanged -> _uiState.update {
                it.copy(
                    notebookName = event.name,
                    errorMessage = null
                )
            }

            is CreateNotebookEvent.RemoteUrlChanged -> _uiState.update {
                it.copy(
                    remoteUrl = event.url.trim(),
                    errorMessage = null
                )
            }

            is CreateNotebookEvent.UsernameChanged -> _uiState.update {
                it.copy(
                    username = event.username,
                    errorMessage = null
                )
            }

            is CreateNotebookEvent.PasswordChanged -> _uiState.update {
                it.copy(
                    password = event.password,
                    errorMessage = null
                )
            }

            is CreateNotebookEvent.DirectorySelected -> _uiState.update {
                it.copy(
                    selectedDirectory = event.directory,
                    errorMessage = null
                )
            }

            is CreateNotebookEvent.CreateLocalNotebook -> createLocalNotebook(event.path, event.onSuccess)
            is CreateNotebookEvent.CloneRemoteNotebook -> cloneRemoteNotebook(event.path, event.onSuccess)
            is CreateNotebookEvent.OpenExistingNotebook -> openExistingNotebook(event.onSuccess)
            is CreateNotebookEvent.AddErrorMessage -> _uiState.update { it.copy(errorMessage = event.message) }
        }
    }

    private fun createLocalNotebook(actualDirectory: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val currentState = _uiState.value


        viewModelScope.launch {
            val result = notebookRepository.createNotebook(
                name = currentState.notebookName,
                directoryPath = actualDirectory
            )

            handleNotebookAddResult(result, onSuccess)
        }
    }

    private fun cloneRemoteNotebook(actualDirectory: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val currentState = _uiState.value

        viewModelScope.launch {
            val result = notebookRepository.cloneNotebook(
                name = currentState.notebookName,
                directoryPath = actualDirectory,
                remoteUrl = currentState.remoteUrl,
                remotePasswordOrToken = currentState.password,
                remoteUsername = currentState.username
            )

            handleNotebookAddResult(result, onSuccess)
        }
    }


    private fun openExistingNotebook(onSuccess: () -> Unit) {
        val selectedDirectory = _uiState.value.selectedDirectory ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = notebookRepository.openExistingNotebook(
                directoryPath = selectedDirectory
            )

            handleNotebookAddResult(result, onSuccess, isCreated = false)
        }
    }



    private suspend fun handleNotebookAddResult(result: Result<Notebook>, onSuccess: () -> Unit, isCreated: Boolean = true) {
        result.onSuccess { notebook ->
            // reset the dialog fields first, so activation below can still report an error
            _uiState.update {
                it.copy(
                    notebookName = "",
                    selectedDirectory = null,
                    remoteUrl = "",
                    username = "",
                    password = "",
                    errorMessage = null,
                    isLoading = false,
                )
            }

            // activate the notebook in repository
            notebookRepository.activateNotebook(notebook.id).onFailure {
                _uiState.update { it.copy(errorMessage = "Failed to activate notebook") }
            }

            snackbarManager.showMessage(SnackbarText.resource(
                if (isCreated)Res.string.msg_notebook_created
                else Res.string.msg_notebook_opened
            ))
            onSuccess()
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    errorMessage = error.message ?: "Failed to create notebook",
                    isLoading = false,
                )
            }
        }
    }

    sealed interface CreateNotebookEvent {
        data class TabSelected(val index: Int) : CreateNotebookEvent
        data class NotebookNameChanged(val name: String) : CreateNotebookEvent
        data class RemoteUrlChanged(val url: String) : CreateNotebookEvent
        data class UsernameChanged(val username: String) : CreateNotebookEvent
        data class PasswordChanged(val password: String) : CreateNotebookEvent
        data class DirectorySelected(val directory: String) : CreateNotebookEvent
        data class OpenExistingNotebook(val onSuccess: () -> Unit) : CreateNotebookEvent
        data class CreateLocalNotebook(val path: String, val onSuccess: () -> Unit) : CreateNotebookEvent
        data class CloneRemoteNotebook(val path: String, val onSuccess: () -> Unit) : CreateNotebookEvent
        data class AddErrorMessage(val message: String) : CreateNotebookEvent
    }
}
