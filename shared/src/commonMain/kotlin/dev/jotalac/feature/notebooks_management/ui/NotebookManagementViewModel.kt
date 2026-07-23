package dev.jotalac.feature.notebooks_management.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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

class NotebookManagementViewModel(
    private val notebookRepository: NotebookRepository,
    private val notebookPathProvider: NotebookPathProvider
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

    fun onEvent(event: NotebookManagementEvent) {
        when (event) {
            is NotebookManagementEvent.TabSelected -> _uiState.update { it.copy(selectedTabIndex = event.index) }
            is NotebookManagementEvent.NotebookNameChanged -> _uiState.update { it.copy(notebookName = event.name) }
            is NotebookManagementEvent.RemoteUrlChanged -> _uiState.update { it.copy(remoteUrl = event.url) }
            is NotebookManagementEvent.UsernameChanged -> _uiState.update { it.copy(username = event.username) }
            is NotebookManagementEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password) }
            is NotebookManagementEvent.DirectorySelected -> _uiState.update { it.copy(selectedDirectory = event.directory) }
            is NotebookManagementEvent.CreateLocalNotebook -> createLocalNotebook(event.path, event.onSuccess)
            is NotebookManagementEvent.CloneRemoteNotebook -> cloneRemoteNotebook(event.path, event.onSuccess)

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
                activateNotebook(notebook.id)

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

    private fun activateNotebook(notebookId: Int) {
        return
    }

    sealed interface NotebookManagementEvent {
        data class TabSelected(val index: Int) : NotebookManagementEvent
        data class NotebookNameChanged(val name: String) : NotebookManagementEvent
        data class RemoteUrlChanged(val url: String) : NotebookManagementEvent
        data class UsernameChanged(val username: String) : NotebookManagementEvent
        data class PasswordChanged(val password: String) : NotebookManagementEvent
        data class DirectorySelected(val directory: String) : NotebookManagementEvent
        data class CreateLocalNotebook(val path: String, val onSuccess: () -> Unit) : NotebookManagementEvent
        data class CloneRemoteNotebook(val path: String, val onSuccess: () -> Unit) : NotebookManagementEvent
    }
}
