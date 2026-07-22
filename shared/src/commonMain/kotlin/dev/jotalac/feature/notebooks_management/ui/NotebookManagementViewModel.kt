package dev.jotalac.feature.notebooks_management.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CreateNotebookState(
    val selectedTabIndex: Int = 0,
    val notebookName: String = "",
    val remoteUrl: String = "",
    val username: String = "",
    val password: String = "",
    val selectedDirectory: String? = null
)

class NotebookManagementViewModel : ViewModel() {
    private val _state = MutableStateFlow(CreateNotebookState())
    val state: StateFlow<CreateNotebookState> = _state.asStateFlow()

    fun onTabSelected(index: Int) {
        _state.update { it.copy(selectedTabIndex = index) }
    }

    fun onNotebookNameChange(name: String) {
        _state.update { it.copy(notebookName = name) }
    }

    fun onRemoteUrlChange(url: String) {
        _state.update { it.copy(remoteUrl = url) }
    }

    fun onUsernameChange(username: String) {
        _state.update { it.copy(username = username) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onDirectorySelected(directory: String) {
        _state.update { it.copy(selectedDirectory = directory) }
    }

    fun createLocalNotebook(actualDirectory: String) {
        // TODO: Implement notebook creation logic
        val currentState = _state.value
        println("Creating local notebook: ${currentState.notebookName} at $actualDirectory")
    }

    fun cloneRemoteNotebook(actualDirectory: String) {
        // TODO: Implement remote cloning logic
        val currentState = _state.value
        println("Cloning remote notebook: ${currentState.remoteUrl} to $actualDirectory")
    }
}
