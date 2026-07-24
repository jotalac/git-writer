package dev.jotalac.feature.notebooks_management.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.database.AppPreferencesManager
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotebookListState(
    val notebooks: List<Notebook> = emptyList(),
    val errorMessage: String? = null
)

class NotebookListViewModel(
    private val notebookRepository: NotebookRepository,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotebookListState())
    val uiState: StateFlow<NotebookListState> = _uiState.asStateFlow()

    init {
        notebookRepository.getAllNotebooks()
            .onEach { notebooks ->
                _uiState.update { it.copy(notebooks = notebooks, errorMessage = null) }
            }
            .catch { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to load notebooks") }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: NotebookListEvent) {
        when (event) {
            is NotebookListEvent.OpenNotebook -> openNotebook(event.id, event.onSuccess)
            is NotebookListEvent.DeleteNotebook -> deleteNotebook(event.id)
        }
    }

    private fun openNotebook(id: Long, onSuccess: () -> Unit) {
        // Add activation logic here if needed, e.g.,
        viewModelScope.launch {
         val result = notebookRepository.activateNotebook(id)

            result
                .onSuccess {
                    onSuccess()
                    _uiState.update { it.copy(errorMessage = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Failed to open notebook") }
                }
        }

    }

    private fun deleteNotebook(id: Long) {
        viewModelScope.launch {
            val result = notebookRepository.deleteNotebook(id)
            result
                .onSuccess {
                    snackbarManager.showMessage("Notebook deleted successfully")
                    _uiState.update { it.copy(errorMessage = null) }

                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Failed to delete notebook") }
                }
        }
    }

    sealed interface NotebookListEvent {
        data class OpenNotebook(val id: Long, val onSuccess: () -> Unit) : NotebookListEvent
        data class DeleteNotebook(val id: Long) : NotebookListEvent
    }
}
