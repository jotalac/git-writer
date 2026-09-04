package dev.jotalac.feature.editor.ui

import dev.jotalac.core.data.UserSettingsManager
import dev.jotalac.core.domain.GitConflictResolutionStrategy
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update

/**
 * Orchestrates git sync and merge-conflict resolution for the editor screen.
 * Reads/writes [state] and reports results through [snackbarManager].
 */
class EditorSyncController(
    private val gitSyncRepository: GitSyncRepository,
    private val notebookRepository: NotebookRepository,
    private val snackbarManager: SnackbarManager,
    private val userSettingsManager: UserSettingsManager,
    private val state: MutableStateFlow<EditorScreenState>,
    private val saveCurrentNote: suspend () -> Unit,
    private val reloadNote: suspend (String) -> Unit,
) {

    suspend fun sync() {
        val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return
        if (notebook.remoteUrl.isNullOrBlank() || notebook.remotePassword.isNullOrBlank()) return

        state.update { it.copy(gitSyncStatus = GitSyncStatus.Syncing) }
        saveCurrentNote()

        val result = gitSyncRepository.syncNotes(
            notebook.directoryPath,
            notebook.remotePassword,
            notebook.remoteUsername
        )

        result.onSuccess { syncResult ->
            if (syncResult is GitSyncStatus.Conflict) {
                val strategy = userSettingsManager.userSettingsStateFlow.first().gitConflictStrategy
                if (strategy != GitConflictResolutionStrategy.MANUAL) {
                    executeResolveAllConflicts(
                        notebookPath = notebook.directoryPath,
                        remotePassword = notebook.remotePassword,
                        remoteUsername = notebook.remoteUsername,
                        keepLocal = strategy == GitConflictResolutionStrategy.LOCAL
                    )
                } else {
                    state.update {
                        it.copy(conflictedFiles = syncResult.files.toList(), gitSyncStatus = syncResult)
                    }
                }
            } else {
                state.value.activeNotePath?.let { reloadNote(it) }
                state.update { it.copy(gitSyncStatus = GitSyncStatus.UpToDate) }
                snackbarManager.showMessage("Notes synced successfully")
            }
        }.onFailure { errorResult ->
            state.update { it.copy(gitSyncStatus = GitSyncStatus.GitSyncFailed) }
            snackbarManager.showMessage(errorResult.message ?: "Error syncing notes")
        }
    }

    suspend fun resolveSingleConflict(filePath: String, keepLocal: Boolean) {
        val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return
        val result = gitSyncRepository.resolveSingleConflict(
            currentNotebookPath = notebook.directoryPath,
            conflictedFilePath = filePath,
            keepLocalChanges = keepLocal
        )

        result.onSuccess {
            val remaining = state.value.conflictedFiles.filter { it != filePath }
            state.update { it.copy(conflictedFiles = remaining) }

            // if the resolved conflict was on the opened note - refresh it
            val activeNotePath = state.value.activeNotePath
            if (activeNotePath != null && (activeNotePath.endsWith(filePath) || state.value.activeFilename == filePath)) {
                reloadNote(activeNotePath)
            }

            if (remaining.isEmpty()) {
                if (!notebook.remotePassword.isNullOrBlank()) {
                    gitSyncRepository.pushChanges(
                        currentNotebookPath = notebook.directoryPath,
                        tokenOrPassword = notebook.remotePassword,
                        username = notebook.remoteUsername
                    )
                }
                state.update { it.copy(gitSyncStatus = GitSyncStatus.UpToDate) }
                snackbarManager.showMessage("All conflicts resolved and synced")
            }
        }.onFailure {
            snackbarManager.showMessage(it.message ?: "Failed to resolve conflict")
        }
    }

    suspend fun resolveAllConflicts(keepLocal: Boolean) {
        val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return
        executeResolveAllConflicts(
            notebookPath = notebook.directoryPath,
            remotePassword = notebook.remotePassword,
            remoteUsername = notebook.remoteUsername,
            keepLocal = keepLocal
        )
    }

    suspend fun abort() {
        val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return
        val result = gitSyncRepository.abortMerge(notebook.directoryPath)

        result.onSuccess {
            snackbarManager.showMessage("Sync aborted")
            state.update { it.copy(conflictedFiles = emptyList(), gitSyncStatus = GitSyncStatus.UpToDate) }
        }.onFailure {
            snackbarManager.showMessage(it.message ?: "Failed to abort sync")
        }
    }

    private suspend fun executeResolveAllConflicts(
        notebookPath: String,
        remotePassword: String?,
        remoteUsername: String?,
        keepLocal: Boolean
    ) {
        val result = gitSyncRepository.resolveAllConflicts(
            currentNotebookPath = notebookPath,
            keepLocalChanges = keepLocal
        )

        result.onSuccess {
            state.update {
                it.copy(conflictedFiles = emptyList(), gitSyncStatus = GitSyncStatus.UpToDate)
            }

            state.value.activeNotePath?.let { reloadNote(it) }

            if (!remotePassword.isNullOrBlank()) {
                gitSyncRepository.pushChanges(
                    currentNotebookPath = notebookPath,
                    tokenOrPassword = remotePassword,
                    username = remoteUsername
                )
            }
            snackbarManager.showMessage("All conflicts resolved and synced")
        }.onFailure {
            state.update { it.copy(gitSyncStatus = GitSyncStatus.GitSyncFailed) }
            snackbarManager.showMessage(it.message ?: "Failed to resolve conflicts")
        }
    }
}
