package dev.jotalac.feature.notebooks_management.domain

import kotlinx.coroutines.flow.Flow

interface NotebookRepository {
    fun getAllNotebooks(): Flow<List<Notebook>>
    suspend fun createNotebook(
        name: String, directoryPath: String,
    ): Result<Notebook>

    suspend fun openExistingNotebook(
        directoryPath: String,
    ): Result<Notebook>

    /** [encryptionPassword] opens a remote that is an encrypted notebook - it is ignored for any other. */
    suspend fun cloneNotebook(
        name: String, directoryPath: String,
        remoteUrl: String, remotePasswordOrToken: String, remoteUsername: String? = null,
        encryptionPassword: String? = null,
    ): Result<Notebook>


    suspend fun deleteNotebook(id: Long): Result<Unit>

    suspend fun updateNotebook(
        id: Long,
        name: String,
        remoteUrl: String?,
        remoteUsername: String?,
        remotePassword: String?,
        encryptionPassword: String? = null, // turns on encryption - the remote has to be empty
    ): Result<Notebook>

    val activeNotebookState: Flow<Notebook?>
    val activeNotePath: Flow<String?>
    fun getNotebookByIdAsFlow(id: Long): Flow<Notebook?>
    suspend fun activateNotebook(id: Long): Result<Notebook>
    suspend fun activateNote(notePath: String): Result<Unit>
    suspend fun closeActiveNote(): Result<Unit>
    suspend fun syncActiveNotePathOnMoved(oldPath: String, newPath: String): Result<Unit>
    suspend fun syncActiveNotePathOnDeleted(deletedPath: String): Result<Unit>
    suspend fun isNotebookNameUnique(name: String, excludeId: Long? = null): Boolean
    fun createBaseNotebooksDirectory(basePath: String): Result<Unit>
}