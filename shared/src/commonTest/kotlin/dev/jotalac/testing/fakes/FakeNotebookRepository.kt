package dev.jotalac.testing.fakes

import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeNotebookRepository : NotebookRepository {

    val activeNotebook = MutableStateFlow<Notebook?>(null)
    val activeNote = MutableStateFlow<String?>(null)
    val notebooks = MutableStateFlow<List<Notebook>>(emptyList())

    override val activeNotebookState: Flow<Notebook?> = activeNotebook
    override val activeNotePath: Flow<String?> = activeNote

    /** Makes loading the notebook list fail. */
    var notebooksError: Throwable? = null

    override fun getAllNotebooks(): Flow<List<Notebook>> =
        notebooksError?.let { error -> flow { throw error } } ?: notebooks

    override fun getNotebookByIdAsFlow(id: Long): Flow<Notebook?> =
        notebooks.map { list -> list.firstOrNull { it.id == id } }

    /** Only tests that care about a returned notebook need to set this. */
    var notebookResult: Result<Notebook> = Result.failure(IllegalStateException("not stubbed"))

    var deleteResult: Result<Unit> = Result.success(Unit)
    /** What activateNotebook returns, [notebookResult] when not set. */
    var activateResult: Result<Notebook>? = null
    var baseDirectoryResult: Result<Unit> = Result.success(Unit)
    val createdBaseDirectories = mutableListOf<String>()

    /** Names [isNotebookNameUnique] reports as taken. */
    val takenNames: MutableSet<String> = mutableSetOf()

    val activatedNotebookIds = mutableListOf<Long>()
    val activatedNotes = mutableListOf<String>()
    val deletedNotebookIds = mutableListOf<Long>()
    val updatedNotebookIds = mutableListOf<Long>()
    val notebookUpdates = mutableListOf<NotebookUpdate>()
    val clonedNotebooks = mutableListOf<ClonedNotebook>()

    data class NotebookUpdate(
        val id: Long, val name: String, val remoteUrl: String?, val remoteUsername: String?, val remotePassword: String?,
        val encryptionPassword: String? = null,
    )

    data class ClonedNotebook(
        val name: String, val directoryPath: String, val remoteUrl: String, val password: String, val username: String?,
        val encryptionPassword: String? = null,
    )
    val closedActiveNotes = 0

    override suspend fun createNotebook(name: String, directoryPath: String): Result<Notebook> =
        notebookResult

    override suspend fun openExistingNotebook(directoryPath: String): Result<Notebook> = notebookResult

    override suspend fun cloneNotebook(
        name: String,
        directoryPath: String,
        remoteUrl: String,
        remotePasswordOrToken: String,
        remoteUsername: String?,
        encryptionPassword: String?,
    ): Result<Notebook> {
        clonedNotebooks += ClonedNotebook(name, directoryPath, remoteUrl, remotePasswordOrToken, remoteUsername, encryptionPassword)
        return notebookResult
    }

    override suspend fun deleteNotebook(id: Long): Result<Unit> {
        deletedNotebookIds += id
        return deleteResult
    }

    override suspend fun updateNotebook(
        id: Long,
        name: String,
        remoteUrl: String?,
        remoteUsername: String?,
        remotePassword: String?,
        encryptionPassword: String?,
    ): Result<Notebook> {
        updatedNotebookIds += id
        notebookUpdates += NotebookUpdate(id, name, remoteUrl, remoteUsername, remotePassword, encryptionPassword)
        return notebookResult
    }

    override suspend fun activateNotebook(id: Long): Result<Notebook> {
        activatedNotebookIds += id
        return activateResult ?: notebookResult
    }

    /**
     * Mirrors the real repository's observable effect, which is the one thing a fake may do: the
     * ViewModel reacts to the flow, not to the returned [Result].
     */
    override suspend fun activateNote(notePath: String): Result<Unit> {
        activatedNotes += notePath
        activeNote.value = notePath
        return Result.success(Unit)
    }

    override suspend fun closeActiveNote(): Result<Unit> {
        activeNote.value = null
        return Result.success(Unit)
    }

    override suspend fun syncActiveNotePathOnMoved(oldPath: String, newPath: String): Result<Unit> {
        if (activeNote.value == oldPath) activeNote.value = newPath
        return Result.success(Unit)
    }

    override suspend fun syncActiveNotePathOnDeleted(deletedPath: String): Result<Unit> {
        if (activeNote.value == deletedPath) activeNote.value = null
        return Result.success(Unit)
    }

    override suspend fun isNotebookNameUnique(name: String, excludeId: Long?): Boolean =
        name !in takenNames

    override fun createBaseNotebooksDirectory(basePath: String): Result<Unit> {
        createdBaseDirectories += basePath
        return baseDirectoryResult
    }
}
