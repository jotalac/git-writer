package dev.jotalac.feature.notebooks_management.ui.edit_notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import dev.jotalac.feature.notebooks_management.ui.validateRemoteUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditNotebookState(
    val notebookName: String = "",
    val remoteUrl: String = "",
    val remoteUsername: String = "",
    val remotePassword: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
)

class EditNotebookViewModel(
    private val notebookRepository: NotebookRepository,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditNotebookState())
    val uiState: StateFlow<EditNotebookState> = _uiState.asStateFlow()

    private var notebookId: Long = 0

    fun initWithNotebook(id: Long, name: String, remoteUrl: String?, remoteUsername: String?, remotePassword: String?) {
        notebookId = id
        _uiState.update {
            it.copy(
                notebookName = name,
                remoteUrl = remoteUrl ?: "",
                remoteUsername = remoteUsername ?: "",
                remotePassword = remotePassword ?: "",
                errorMessage = null,
                isLoading = false,
            )
        }
    }

    fun onEvent(event: EditNotebookEvent) {
        when (event) {
            is EditNotebookEvent.NameChanged -> _uiState.update {
                it.copy(
                    notebookName = event.name,
                    errorMessage = null
                )
            }

            is EditNotebookEvent.RemoteUrlChanged -> _uiState.update {
                it.copy(
                    remoteUrl = event.url,
                    errorMessage = null
                )
            }

            is EditNotebookEvent.UsernameChanged -> _uiState.update {
                it.copy(
                    remoteUsername = event.username,
                    errorMessage = null
                )
            }

            is EditNotebookEvent.PasswordChanged -> _uiState.update {
                it.copy(
                    remotePassword = event.password,
                    errorMessage = null
                )
            }

            is EditNotebookEvent.SaveNotebook -> saveNotebookEdit(event.onSuccess)
            is EditNotebookEvent.ClearRemoteCredentials -> _uiState.update {
                it.copy(
                    remoteUrl = "",
                    remoteUsername = "",
                    remotePassword = "",
                )
            }
        }
    }

    private fun validateForm(state: EditNotebookState): String? {
        if (state.notebookName.isBlank()) {
            return "Notebook name cannot be empty"
        }

        val hasRemoteUrl = state.remoteUrl.isNotBlank()
        val hasUsername = state.remoteUsername.isNotBlank()
        val hasPassword = state.remotePassword.isNotBlank()

        if (hasRemoteUrl || hasUsername || hasPassword) {
            if (!hasRemoteUrl) {
                return "Please fill in repository URL"
            }
            if (!hasUsername || !hasPassword) {
                return "Please fill in all remote credential fields"
            }
            val urlError = validateRemoteUrl(state.remoteUrl.trim())
            if (urlError != null) {
                return urlError
            }
        }

        return null
    }

    private fun saveNotebookEdit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentState = _uiState.value

            val validationError = validateForm(currentState)
            if (validationError != null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = validationError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val updateNotebookResult = notebookRepository.updateNotebook(
                id = notebookId,
                name = currentState.notebookName.trim(),
                remoteUrl = currentState.remoteUrl.trim().ifBlank { null },
                remoteUsername = currentState.remoteUsername.trim().ifBlank { null },
                remotePassword = currentState.remotePassword.ifBlank { null },
            )

            updateNotebookResult.onSuccess {
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                snackbarManager.showMessage("Notebook updated successfully")
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to update notebook",
                        isLoading = false,
                    )
                }
            }
        }
    }

    sealed interface EditNotebookEvent {
        data class NameChanged(val name: String) : EditNotebookEvent
        data class RemoteUrlChanged(val url: String) : EditNotebookEvent
        data class UsernameChanged(val username: String) : EditNotebookEvent
        data class PasswordChanged(val password: String) : EditNotebookEvent
        data class SaveNotebook(val onSuccess: () -> Unit) : EditNotebookEvent
        data object ClearRemoteCredentials : EditNotebookEvent
    }
}
