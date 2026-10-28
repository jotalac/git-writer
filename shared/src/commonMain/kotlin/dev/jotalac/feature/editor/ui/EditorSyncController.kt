package dev.jotalac.feature.editor.ui

import dev.jotalac.core.data.UserSettings
import dev.jotalac.core.domain.GitConflictResolutionStrategy
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.git_sync.encryption.EncryptedGitSync
import dev.jotalac.feature.git_sync.encryption.NotebookKey
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.err_failed_abort_sync
import git_writer.shared.generated.resources.err_failed_resolve_conflict
import git_writer.shared.generated.resources.err_failed_resolve_conflicts
import git_writer.shared.generated.resources.err_failed_sync
import git_writer.shared.generated.resources.err_push_after_resolve
import git_writer.shared.generated.resources.msg_conflicts_resolved
import git_writer.shared.generated.resources.msg_notes_synced
import git_writer.shared.generated.resources.msg_sync_aborted

/**
 * Orchestrates git sync and merge-conflict resolution for the editor screen.
 * Reads/writes [state] and reports results through [snackbarManager].
 */
class EditorSyncController(
    private val gitSyncRepository: GitSyncRepository,
    private val encryptedGitSync: EncryptedGitSync,
    private val notebookRepository: NotebookRepository,
    private val snackbarManager: SnackbarManager,
    private val userSettingsManager: UserSettings,
    private val state: MutableStateFlow<EditorScreenState>,
    private val saveCurrentNote: suspend () -> Unit,
    private val reloadNote: suspend (String) -> Unit,
) {

    // automatic syncs start on their own - two git operations on the same repository at once would fail,
    // so a sync started while another one runs waits for it
    private val syncMutex = Mutex()

    suspend fun sync(): Unit = syncMutex.withLock {
        val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return
        if (notebook.remoteUrl.isNullOrBlank() || notebook.remotePassword.isNullOrBlank()) return

        state.update { it.copy(gitSyncStatus = GitSyncStatus.Syncing) }
        saveCurrentNote()

        val result = syncNotebook(notebook, notebook.remotePassword)

        result.onSuccess { syncResult ->
            if (syncResult is GitSyncStatus.Conflict) {
                val strategy = userSettingsManager.userSettingsStateFlow.first().gitConflictStrategy
                if (strategy != GitConflictResolutionStrategy.MANUAL) {
                    executeResolveAllConflicts(notebook, keepLocal = strategy == GitConflictResolutionStrategy.LOCAL)
                } else {
                    state.update {
                        it.copy(conflictedFiles = syncResult.files.toList(), gitSyncStatus = syncResult)
                    }
                }
            } else {
                state.value.activeNotePath?.let { reloadNote(it) }
                state.update { it.copy(gitSyncStatus = GitSyncStatus.UpToDate) }
                snackbarManager.showMessage(SnackbarText.resource(Res.string.msg_notes_synced))
            }
        }.onFailure { errorResult ->
            state.update { it.copy(gitSyncStatus = GitSyncStatus.GitSyncFailed) }
            snackbarManager.showMessage(SnackbarText.message(Res.string.err_failed_sync, errorResult.message))
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
                pushResolvedConflicts(notebook)
            }
        }.onFailure {
            snackbarManager.showMessage(SnackbarText.message(Res.string.err_failed_resolve_conflict, it.message))
        }
    }

    suspend fun resolveAllConflicts(keepLocal: Boolean) {
        val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return
        executeResolveAllConflicts(notebook, keepLocal)
    }

    suspend fun abort() {
        val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return
        val result = gitSyncRepository.abortMerge(notebook.directoryPath)

        result.onSuccess {
            snackbarManager.showMessage(SnackbarText.resource(Res.string.msg_sync_aborted))
            state.update { it.copy(conflictedFiles = emptyList(), gitSyncStatus = GitSyncStatus.UpToDate) }
        }.onFailure {
            snackbarManager.showMessage(SnackbarText.message(Res.string.err_failed_abort_sync, it.message))
        }
    }

    private suspend fun executeResolveAllConflicts(notebook: Notebook, keepLocal: Boolean) {
        val result = gitSyncRepository.resolveAllConflicts(
            currentNotebookPath = notebook.directoryPath,
            keepLocalChanges = keepLocal
        )

        result.onSuccess {
            state.update { it.copy(conflictedFiles = emptyList()) }

            state.value.activeNotePath?.let { reloadNote(it) }

            pushResolvedConflicts(notebook)
        }.onFailure {
            state.update { it.copy(gitSyncStatus = GitSyncStatus.GitSyncFailed) }
            snackbarManager.showMessage(SnackbarText.message(Res.string.err_failed_resolve_conflicts, it.message))
        }
    }

    // the resolution is already committed locally, so a failed push is not lost - the next sync pushes it
    private suspend fun pushResolvedConflicts(notebook: Notebook) {
        val token = notebook.remotePassword
        val pushResult = when {
            token.isNullOrBlank() -> Result.success(Unit)
            // the resolution is a merge commit the encrypted sync publishes like any other
            notebook.isEncrypted -> syncNotebook(notebook, token).map { status ->
                check(status !is GitSyncStatus.Conflict) { "Another device changed the same notes meanwhile - sync again" }
            }
            else -> gitSyncRepository.pushChanges(
                currentNotebookPath = notebook.directoryPath,
                tokenOrPassword = token,
                username = notebook.remoteUsername
            )
        }

        pushResult.onSuccess {
            state.update { it.copy(gitSyncStatus = GitSyncStatus.UpToDate) }
            snackbarManager.showMessage(SnackbarText.resource(Res.string.msg_conflicts_resolved))
        }.onFailure {
            state.update { it.copy(gitSyncStatus = GitSyncStatus.GitSyncFailed) }
            snackbarManager.showMessage(SnackbarText.message(Res.string.err_push_after_resolve, it.message))
        }
    }

    // an encrypted notebook's remote only takes encrypted bundles - see EncryptedGitSync
    private suspend fun syncNotebook(notebook: Notebook, token: String): Result<GitSyncStatus> {
        if (!notebook.isEncrypted) return gitSyncRepository.syncNotes(notebook.directoryPath, token, notebook.remoteUsername)

        val key = notebook.encryptionKey
            ?: return Result.failure(IllegalStateException("This device has no key for the encrypted notebook"))
        return encryptedGitSync.sync(notebook.directoryPath, NotebookKey.fromEncoded(key), token, notebook.remoteUsername)
    }
}
