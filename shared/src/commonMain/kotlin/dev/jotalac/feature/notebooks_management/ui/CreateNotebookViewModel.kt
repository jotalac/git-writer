package dev.jotalac.feature.notebooks_management.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.utils.SnackbarManager
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
    val errorMessage: String? = null
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
            is CreateNotebookEvent.TabSelected -> _uiState.update { it.copy(selectedTabIndex = event.index) }
            is CreateNotebookEvent.NotebookNameChanged -> _uiState.update { it.copy(notebookName = event.name) }
            is CreateNotebookEvent.RemoteUrlChanged -> _uiState.update { it.copy(remoteUrl = event.url) }
            is CreateNotebookEvent.UsernameChanged -> _uiState.update { it.copy(username = event.username) }
            is CreateNotebookEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password) }
            is CreateNotebookEvent.DirectorySelected -> _uiState.update { it.copy(selectedDirectory = event.directory) }
            is CreateNotebookEvent.CreateLocalNotebook -> createLocalNotebook(event.path, event.onSuccess)
            is CreateNotebookEvent.CloneRemoteNotebook -> cloneRemoteNotebook(event.path, event.onSuccess)

        }
    }

    private fun ensureDirectoryExists(directoryPath: String) {
        val directory = PlatformFile(directoryPath)
        if (!directory.exists()) {
            directory.createDirectories()
        }
    }

    private fun createLocalNotebook(actualDirectory: String, onSuccess: () -> Unit) {
        val currentState = _uiState.value


        viewModelScope.launch {
            val result = notebookRepository.createLocalNotebook(
                name = currentState.notebookName,
                directoryPath = actualDirectory,
                remoteUrl = currentState.remoteUrl,
                remoteUsername = currentState.username,
                remotePassword = currentState.password,
            )

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
                        errorMessage = null
                    )
                }

                snackbarManager.showMessage("Notebook created successfully")
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to create notebook"
                    )
                }
            }
        }
    }

    private fun cloneRemoteNotebook(actualDirectory: String, onSuccess: () -> Unit) {
        // TODO: Implement remote cloning logic
        val currentState = _uiState.value
        
        _uiState.update {
            it.copy(
                notebookName = "",
                remoteUrl = "",
                username = "",
                password = "",
            )
        }
        
        onSuccess()
        
        println("Cloning remote notebook: ${currentState.remoteUrl} to $actualDirectory")
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
    }
}
