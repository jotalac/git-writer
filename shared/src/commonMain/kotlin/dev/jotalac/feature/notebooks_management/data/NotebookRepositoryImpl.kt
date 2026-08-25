package dev.jotalac.feature.notebooks_management.data

import dev.jotalac.core.data.ActiveNotebookManager
import dev.jotalac.core.utils.deleteRecursively
import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.core.utils.toSafeFileName
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class NotebookRepositoryImpl(
    private val notebookDao: NotebookDao,
    private val activeNotebookManager: ActiveNotebookManager,
    private val gitSyncRepository: GitSyncRepository
) : NotebookRepository {
    override fun getAllNotebooks(): Flow<List<Notebook>> {
        return notebookDao.getNotebooksAsFlow().map { entityList ->
            entityList.map { entity -> entity.toNotebook() }
        }
    }


    private fun directoryExists(path: String): Boolean {
        val file = PlatformFile(path)
        return file.exists() && file.isDirectory()
    }

    override suspend fun createNotebook(
        name: String, directoryPath: String,
    ): Result<Notebook> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            // first create the directory
            if (directoryExists(directoryPath)) {
                throw IllegalStateException("Directory already exists")
            }
            val baseDirectory = PlatformFile(directoryPath)
            baseDirectory.createDirectories()

            //create base images directory
            (baseDirectory / "images").createDirectories()

            //initialize git repo
            gitSyncRepository.initRepository(directoryPath)

            //save notebook to database
            val notebook = Notebook(
                name = name.trim(),
                directoryPath = directoryPath,
                remoteUrl = null,
                remoteUsername = null,
                remotePassword = null,
            )
            val generatedId = notebookDao.upsertNotebook(notebook.toNotebookEntity())

            notebook.copy(id = generatedId)
        }
    }

    override suspend fun cloneNotebook(
        name: String,
        directoryPath: String,
        remoteUrl: String,
        remotePasswordOrToken: String,
        remoteUsername: String?,
    ): Result<Notebook> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            // first create the directory
            if (directoryExists(directoryPath)) {
                throw IllegalStateException("Directory already exists")
            }
            val baseDirectory = PlatformFile(directoryPath)
            baseDirectory.createDirectories()

            // validate remote credentials
            val credentialsVerificationResult =
                gitSyncRepository.validateCredentials(remoteUrl, remotePasswordOrToken, remoteUsername)
            if (credentialsVerificationResult.isFailure) {
                println(credentialsVerificationResult.exceptionOrNull())

                baseDirectory.delete()
                throw IllegalStateException("Invalid remote credentials")
            }

            // clone the repository
            val repoCloneResult = gitSyncRepository.cloneRepository(
                remoteUrl,
                remotePasswordOrToken,
                directoryPath,
                remoteUsername
            )

            if (repoCloneResult.isFailure) {
                baseDirectory.delete()
                throw IllegalStateException("Failed to clone repository")
            }
            // save notebook to database
            val notebook = Notebook(
                name = name.trim(),
                directoryPath = directoryPath,
                remoteUrl = remoteUrl,
                remoteUsername = remoteUsername,
                remotePassword = remotePasswordOrToken,
            )
            val generatedId = notebookDao.upsertNotebook(notebook.toNotebookEntity())

            notebook.copy(id = generatedId)
        }
    }

    override suspend fun deleteNotebook(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val notebook =
                notebookDao.getNotebookById(id) ?: throw NullPointerException("Notebook with id $id not found")

            // delete the directory and all files inside
            val notebookDirectoryPath = Path(notebook.directoryPath)

            notebookDirectoryPath.deleteRecursively()

            notebookDao.deleteNotebook(notebook.id)

            if (activeNotebookManager.activeNotebookStateFlow.firstOrNull()?.notebookId == id) {
                activeNotebookManager.clearActiveNotebook()
            }
        }
    }

    override suspend fun updateNotebook(
        id: Long,
        name: String,
        remoteUrl: String?,
        remoteUsername: String?,
        remotePassword: String?,
    ): Result<Notebook> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val existing = notebookDao.getNotebookById(id)
                ?: throw NullPointerException("Notebook with id $id not found")

            //validate the name
            val newDirectoryPath = validateUpdateNotebookName(name, existing)

            //validate the remote credentials - if changed
            if (!remoteUrl.isNullOrBlank() && !remotePassword.isNullOrBlank()
                && (remoteUrl != existing.remoteUrl || remoteUsername != existing.remoteUsername || remotePassword != existing.remotePassword)
            ) {
                validateUpdateNotebookRemote(remoteUrl, remoteUsername, remotePassword)
                //update the git remote, when valid
                gitSyncRepository.updateRemoteUrl(existing.directoryPath, remoteUrl)
            }

            // remove the remote if it was deleted
            if (remoteUrl.isNullOrBlank() && remoteUrl != existing.remoteUrl) gitSyncRepository.removeRemote(existing.directoryPath)

            gitSyncRepository.updateSyncStatus(remoteUrl)

            //rename the folder name to match the notebook name
            moveNotebookPaths(existing.directoryPath, newDirectoryPath)

            val updated = existing.copy(
                name = name,
                directoryPath = newDirectoryPath,
                remoteUrl = remoteUrl,
                remoteUsername = remoteUsername,
                remotePassword = remotePassword,
            )
            notebookDao.upsertNotebook(updated)

            updated.toNotebook()
        }
    }

    private suspend fun validateUpdateNotebookName(name: String, existingNotebook: NotebookEntity): String {
        // validate name uniqueness (excluding this notebook)
        if (!isNotebookNameUnique(name, excludeId = existingNotebook.id)) {
            throw IllegalStateException("A notebook with this name already exists")
        }

        var newDirectoryPath = existingNotebook.directoryPath

        // if the name changed, rename the folder
        if (existingNotebook.name != name) {
            val safeFolderName = name.toSafeFileName()
            val parentPath = Path(existingNotebook.directoryPath).parent?.toString()
                ?: throw IllegalStateException("Cannot determine parent directory")

            val newPath = "$parentPath/$safeFolderName"

            // check if target directory already exists (different from current)
            if (newPath != existingNotebook.directoryPath && directoryExists(newPath)) {
                throw IllegalStateException("A folder with this name already exists")
            }

            if (newPath != existingNotebook.directoryPath) {
                newDirectoryPath = newPath
            }
        }

        return newDirectoryPath
    }

    private fun moveNotebookPaths(oldPath: String, newPath: String) {
        if (oldPath == newPath) return

        val source = Path(oldPath)
        val destination = Path(newPath)
        SystemFileSystem.atomicMove(source, destination)
    }

    private suspend fun validateUpdateNotebookRemote(
        remoteUrl: String,
        remoteUsername: String?,
        remotePassword: String
    ) {
        val validationResult = gitSyncRepository.validateCredentials(remoteUrl, remotePassword, remoteUsername)
        if (validationResult.isFailure) {
            throw IllegalStateException("Invalid remote credentials")
        }
    }

    override suspend fun activateNotebook(id: Long): Result<Notebook> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val notebook = notebookDao.getNotebookById(id) ?: throw NullPointerException("Notebook not found")

            // check if the directory with the notebook content still exists
            if (!directoryExists(notebook.directoryPath)) {
                throw IllegalStateException("Notebook directory doesn't exist anymore (consider deleting the notebook)")
            }

            activeNotebookManager.setActiveNotebook(id)
            gitSyncRepository.updateSyncStatus(notebook.remoteUrl)

            notebook.toNotebook()
        }
    }

    override suspend fun activateNote(notePath: String): Result<Unit> {
        return suspendRunCatching {
            activeNotebookManager.setActiveNotePath(notePath)
        }
    }

    override suspend fun closeActiveNote(): Result<Unit> {
        return suspendRunCatching {
            activeNotebookManager.clearActiveNote()
        }
    }

    override suspend fun syncActiveNotePathOnMoved(oldPath: String, newPath: String): Result<Unit> {
        return suspendRunCatching {
            val currentActivePath =
                activeNotebookManager.activeNotebookStateFlow.firstOrNull()?.notePath ?: return@suspendRunCatching
            if (currentActivePath == oldPath) {
                activeNotebookManager.setActiveNotePath(newPath)
            } else if (currentActivePath.startsWith("$oldPath/")) {
                val updatedPath = currentActivePath.replaceFirst(oldPath, newPath)
                activeNotebookManager.setActiveNotePath(updatedPath)
            }
        }
    }

    override suspend fun syncActiveNotePathOnDeleted(deletedPath: String): Result<Unit> {
        return suspendRunCatching {
            val currentActivePath =
                activeNotebookManager.activeNotebookStateFlow.firstOrNull()?.notePath ?: return@suspendRunCatching
            if (currentActivePath == deletedPath || currentActivePath.startsWith("$deletedPath/")) {
                activeNotebookManager.clearActiveNote()
            }
        }
    }

    override fun getNotebookByIdAsFlow(id: Long): Flow<Notebook?> {
        return notebookDao.getNotebookByIdAsFlow(id).map { entity ->
            entity?.toNotebook()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val activeNotebookState: Flow<Notebook?> = activeNotebookManager.activeNotebookStateFlow
        .flatMapLatest { notebookState ->
            if (notebookState?.notebookId == null) {
                flowOf(null)
            } else {
                getNotebookByIdAsFlow(notebookState.notebookId)
            }
        }

    override val activeNotePath: Flow<String?> = activeNotebookManager.activeNotebookStateFlow.map {
        it?.notePath
    }

    override suspend fun isNotebookNameUnique(name: String, excludeId: Long?): Boolean {
        return if (excludeId != null) {
            notebookDao.getNotebookByNameExcludingId(name, excludeId) == null
        } else {
            notebookDao.getNotebookByName(name) == null
        }
    }

}