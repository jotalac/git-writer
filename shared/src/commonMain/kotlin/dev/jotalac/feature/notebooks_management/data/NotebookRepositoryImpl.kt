package dev.jotalac.feature.notebooks_management.data

import dev.jotalac.core.data.ActiveNotebookManager
import dev.jotalac.core.utils.deleteRecursively
import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.core.utils.toSafeFileName
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.encryption.EncryptedGitSync
import dev.jotalac.feature.git_sync.encryption.PasswordRequiredException
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

// `/…/Containers/Data/Application/<uuid>/` — the part of an iOS app path that changes every install
private val APP_CONTAINER_PREFIX = Regex("^.*?/Application/[0-9A-Fa-f-]{36}/")

class NotebookRepositoryImpl(
    private val notebookDao: NotebookDao,
    private val activeNotebookManager: ActiveNotebookManager,
    private val gitSyncRepository: GitSyncRepository,
    private val notebookPathProvider: NotebookPathProvider,
    private val encryptedGitSync: EncryptedGitSync,
) : NotebookRepository {
    private val appContainerPrefix: String?
        get() = APP_CONTAINER_PREFIX.find(notebookPathProvider.getDefaultNotebookDirectory())?.value

    /* 
    * ios changes directory path per install, in the app we only use the part after the uuid
    * */
    
    //stored in database - without the .../Application/<uuid> ios prefix
    private fun String.toStoredPath(): String =
        APP_CONTAINER_PREFIX.find(this)?.let { removePrefix(it.value) } ?: this

    // in runtime to access the file we need the actual path
    private fun String.toRuntimePath(): String =
        appContainerPrefix?.let { prefix -> prefix + toStoredPath() } ?: this

    private fun NotebookEntity.withStoredPath(): NotebookEntity =
        copy(directoryPath = directoryPath.toStoredPath())

    private fun NotebookEntity.withRuntimePath(): NotebookEntity =
        copy(directoryPath = directoryPath.toRuntimePath())

    override fun getAllNotebooks(): Flow<List<Notebook>> {
        return notebookDao.getNotebooksAsFlow().map { entityList ->
            entityList.map { entity -> entity.withRuntimePath().toNotebook() }
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
            requireUniqueName(name)
            if (directoryExists(directoryPath)) {
                throw IllegalStateException("Directory already exists")
            }

            // create the base directory
            val baseDirectory = PlatformFile(directoryPath)
            baseDirectory.createDirectories()
            val imagesDirectory = (baseDirectory / "images")
            imagesDirectory.createDirectories()

            // make sure the image directory is not dropped on git sync
            (imagesDirectory / ".gitkeep").write(ByteArray(0))


            val gitInit = gitSyncRepository.initRepository(directoryPath)
            if (gitInit.isFailure) {
                Path(directoryPath).deleteRecursively()
                throw IllegalStateException(
                    "Failed to initialize git repository: ${gitInit.exceptionOrNull()?.message}"
                )
            }

            return@suspendRunCatching saveNotebookToDb(name, directoryPath).getOrElse { cause ->
                // delete folder when it fails
                Path(directoryPath).deleteRecursively()
                throw IllegalStateException("Failed to save notebook to database", cause)
            }
        }
    }

    override suspend fun openExistingNotebook(directoryPath: String): Result<Notebook> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                if (!directoryExists(directoryPath)) {
                    throw IllegalStateException("Directory does not exist")
                }

                val rootNotebookDirectory = PlatformFile(directoryPath)

                // a folder that is already a notebook is opened, not registered twice - looked up by its path,
                // another folder may have the same name
                notebookDao.getNotebookByDirectoryPath(directoryPath.toStoredPath())
                    ?.let { return@suspendRunCatching it.withRuntimePath().toNotebook() }

                // init git repo if the folder is not a git repository
                val isGitRepository = (rootNotebookDirectory / ".git").exists()
                val gitInit = if (isGitRepository) {
                    Result.success(Unit)
                } else {
                    gitSyncRepository.initRepository(directoryPath)
                }
                if (gitInit.isFailure) {
                    throw IllegalStateException(
                        "Failed to initialize git repository: ${gitInit.exceptionOrNull()?.message}"
                    )
                }

                saveNotebookToDb(freeNotebookName(rootNotebookDirectory.name), directoryPath).getOrElse { cause ->
                    // delete the .git directory if saving fails - only one created here, an existing one
                    // holds the user's history
                    if (!isGitRepository) Path(directoryPath, ".git").deleteRecursively()
                    throw IllegalStateException("Failed to save notebook to database", cause)
                }
            }
        }

    private suspend fun requireUniqueName(name: String) {
        if (!isNotebookNameUnique(name.trim())) {
            throw IllegalStateException("A notebook with this name already exists")
        }
    }

    // "Diary", or "Diary 2", "Diary 3"... when another notebook already has the name
    private suspend fun freeNotebookName(name: String): String {
        var candidate = name.trim()
        var counter = 1
        while (!isNotebookNameUnique(candidate)) {
            counter++
            candidate = "${name.trim()} $counter"
        }
        return candidate
    }

    private suspend fun saveNotebookToDb(name: String, directoryPath: String): Result<Notebook> =
        suspendRunCatching {
            val notebook = Notebook(
                name = name.trim(),
                directoryPath = directoryPath,
                remoteUrl = null,
                remoteUsername = null,
                remotePassword = null,
            )
            // save the relative path to database (dont save the .../Application/<uuid>/)
            val generatedId = notebookDao.upsertNotebook(notebook.toNotebookEntity().withStoredPath())

            notebook.copy(id = generatedId)
        }

    override suspend fun cloneNotebook(
        name: String,
        directoryPath: String,
        remoteUrl: String,
        remotePasswordOrToken: String,
        remoteUsername: String?,
        encryptionPassword: String?,
    ): Result<Notebook> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            requireUniqueName(name)
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

                Path(directoryPath).deleteRecursively()
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
                Path(directoryPath).deleteRecursively()
                throw IllegalStateException("Failed to clone repository")
            }

            // an encrypted notebook's remote only holds encrypted bundles - it is set up anew with its password
            val encryptionKey = if (EncryptedGitSync.isEncryptedRemote(directoryPath)) {
                Path(directoryPath).deleteRecursively()
                if (encryptionPassword.isNullOrEmpty()) throw PasswordRequiredException()
                baseDirectory.createDirectories()
                encryptedGitSync.open(directoryPath, remoteUrl, remotePasswordOrToken, remoteUsername, encryptionPassword)
                    .getOrElse {
                        Path(directoryPath).deleteRecursively()
                        throw it
                    }
            } else {
                null
            }

            // save notebook to database
            val notebook = Notebook(
                name = name.trim(),
                directoryPath = directoryPath,
                remoteUrl = remoteUrl,
                remoteUsername = remoteUsername,
                remotePassword = remotePasswordOrToken,
                isEncrypted = encryptionKey != null,
                encryptionKey = encryptionKey?.encoded,
            )
            val generatedId = notebookDao.upsertNotebook(notebook.toNotebookEntity().withStoredPath())

            notebook.copy(id = generatedId)
        }
    }

    override suspend fun deleteNotebook(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val notebook = (notebookDao.getNotebookById(id)
                ?: throw NullPointerException("Notebook with id $id not found")).withRuntimePath()

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
        encryptionPassword: String?,
    ): Result<Notebook> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val existing = (notebookDao.getNotebookById(id)
                ?: throw NullPointerException("Notebook with id $id not found")).withRuntimePath()

            //validate the name
            val newDirectoryPath = validateUpdateNotebookName(name, existing)

            val credentialsChanged = remoteUrl != existing.remoteUrl || remoteUsername != existing.remoteUsername ||
                remotePassword != existing.remotePassword
            var encryptionKey = existing.encryptionKey

            when {
                // its git remote is the local mirror - the remote repository holds its bundles and stays
                existing.isEncrypted -> {
                    if (remoteUrl != existing.remoteUrl) {
                        throw IllegalStateException("The remote of an encrypted notebook cannot be changed")
                    }
                    if (credentialsChanged && remoteUrl != null && !remotePassword.isNullOrBlank()) {
                        validateUpdateNotebookRemote(remoteUrl, remoteUsername, remotePassword)
                    }
                }

                encryptionPassword != null -> {
                    if (remoteUrl.isNullOrBlank() || remotePassword.isNullOrBlank()) {
                        throw IllegalStateException("Encryption needs a remote repository")
                    }
                    validateUpdateNotebookRemote(remoteUrl, remoteUsername, remotePassword)
                    encryptionKey = encryptedGitSync
                        .enable(existing.directoryPath, remoteUrl, remotePassword, remoteUsername, encryptionPassword)
                        .getOrThrow()
                        .encoded
                }

                else -> {
                    //validate the remote credentials - if changed
                    if (!remoteUrl.isNullOrBlank() && !remotePassword.isNullOrBlank() && credentialsChanged) {
                        validateUpdateNotebookRemote(remoteUrl, remoteUsername, remotePassword)
                        //update the git remote, when valid
                        gitSyncRepository.updateRemoteUrl(existing.directoryPath, remoteUrl)
                    }

                    // remove the remote if it was deleted
                    if (remoteUrl.isNullOrBlank() && remoteUrl != existing.remoteUrl) gitSyncRepository.removeRemote(existing.directoryPath)
                }
            }

            gitSyncRepository.updateSyncStatus(remoteUrl)

            //rename the folder name to match the notebook name
            moveNotebookPaths(existing.directoryPath, newDirectoryPath)
            // the open note of a renamed notebook would still point into the old folder
            syncActiveNotePathOnMoved(existing.directoryPath, newDirectoryPath)

            val updated = existing.copy(
                name = name,
                directoryPath = newDirectoryPath,
                remoteUrl = remoteUrl,
                remoteUsername = remoteUsername,
                remotePassword = remotePassword,
                isEncrypted = encryptionKey != null,
                encryptionKey = encryptionKey,
            )
            notebookDao.upsertNotebook(updated.withStoredPath())

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
            val notebook = (notebookDao.getNotebookById(id)
                ?: throw NullPointerException("Notebook not found")).withRuntimePath()

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
            activeNotebookManager.setActiveNotePath(notePath.toStoredPath())
        }
    }

    override suspend fun closeActiveNote(): Result<Unit> {
        return suspendRunCatching {
            activeNotebookManager.clearActiveNote()
        }
    }

    override suspend fun syncActiveNotePathOnMoved(oldPath: String, newPath: String): Result<Unit> {
        return suspendRunCatching {
            val storedPath =
                activeNotebookManager.activeNotebookStateFlow.firstOrNull()?.notePath ?: return@suspendRunCatching
            val currentActivePath = storedPath.toRuntimePath()
            if (currentActivePath == oldPath) {
                activeNotebookManager.setActiveNotePath(newPath.toStoredPath())
            } else if (currentActivePath.startsWith("$oldPath/")) {
                val updatedPath = currentActivePath.replaceFirst(oldPath, newPath)
                activeNotebookManager.setActiveNotePath(updatedPath.toStoredPath())
            }
        }
    }

    override suspend fun syncActiveNotePathOnDeleted(deletedPath: String): Result<Unit> {
        return suspendRunCatching {
            val storedPath =
                activeNotebookManager.activeNotebookStateFlow.firstOrNull()?.notePath ?: return@suspendRunCatching
            val currentActivePath = storedPath.toRuntimePath()
            if (currentActivePath == deletedPath || currentActivePath.startsWith("$deletedPath/")) {
                activeNotebookManager.clearActiveNote()
            }
        }
    }

    override fun getNotebookByIdAsFlow(id: Long): Flow<Notebook?> {
        return notebookDao.getNotebookByIdAsFlow(id).map { entity ->
            entity?.withRuntimePath()?.toNotebook()
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
        it?.notePath?.toRuntimePath()
    }

    override suspend fun isNotebookNameUnique(name: String, excludeId: Long?): Boolean {
        return if (excludeId != null) {
            notebookDao.getNotebookByNameExcludingId(name, excludeId) == null
        } else {
            notebookDao.getNotebookByName(name) == null
        }
    }

    override fun createBaseNotebooksDirectory(basePath: String): Result<Unit> {
        val directory = PlatformFile(basePath)
        if (!directory.exists()) {
            directory.createDirectories()
        }

        return Result.success(Unit)
    }

}