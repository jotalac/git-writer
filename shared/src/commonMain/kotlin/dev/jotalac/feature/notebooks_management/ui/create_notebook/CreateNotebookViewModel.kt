package dev.jotalac.feature.notebooks_management.ui.create_notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

        ensureDirectoryExists(basePath)

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
                    remoteUrl = event.url,
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
            is CreateNotebookEvent.AddErrorMessage -> _uiState.update { it.copy(errorMessage = event.message) }
        }
    }

    private fun ensureDirectoryExists(directoryPath: String) {
        val directory = PlatformFile(directoryPath)
        if (!directory.exists()) {
            directory.createDirectories()
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

            handleNotebookCreateResult(result, onSuccess)
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

            handleNotebookCreateResult(result, onSuccess)
        }
    }

    private suspend fun handleNotebookCreateResult(result: Result<Notebook>, onSuccess: () -> Unit) {
        result.onSuccess { notebook ->
            // activate the notebook in repository
            notebookRepository.activateNotebook(notebook.id)

            //reset the dialog values
            _uiState.update {
                it.copy(
                    notebookName = "",
                    remoteUrl = "",
                    username = "",
                    password = "",
                    errorMessage = null,
                    isLoading = false,
                )
            }

            snackbarManager.showMessage("Notebook created successfully")
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
        data class CreateLocalNotebook(val path: String, val onSuccess: () -> Unit) : CreateNotebookEvent
        data class CloneRemoteNotebook(val path: String, val onSuccess: () -> Unit) : CreateNotebookEvent
        data class AddErrorMessage(val message: String) : CreateNotebookEvent
    }
}
