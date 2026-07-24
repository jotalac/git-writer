package dev.jotalac.feature.notebooks_management.domain

import kotlinx.coroutines.flow.Flow

interface NotebookRepository {
    fun getAllNotebooks(): Flow<List<Notebook>>
    suspend fun createLocalNotebook(
        name: String, directoryPath: String,
        remoteUrl: String? = null, remoteUsername: String? = null, remotePassword: String? = null,
    ): Result<Notebook>
    suspend fun deleteNotebook(id: Long): Result<Unit>

    val activeNotebookState: Flow<Notebook?>
    val activeNotePath: Flow<String?>
    fun getNotebookByIdAsFlow(id: Long): Flow<Notebook?>
    suspend fun activateNotebook(id: Long): Result<Unit>
    suspend fun activateNote(notePath: String): Result<Unit>
    suspend fun closeActiveNote(): Result<Unit>

}