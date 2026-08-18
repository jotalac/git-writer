package dev.jotalac.feature.notebooks_management.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {

    @Query("SELECT * FROM notebooks ORDER BY id DESC")
    fun getNotebooksAsFlow(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    fun getNotebookByIdAsFlow(id: Long): Flow<NotebookEntity?>


    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getNotebookById(id: Long): NotebookEntity?

    @Query("SELECT * FROM notebooks WHERE name = :name")
    suspend fun getNotebookByName(name: String): NotebookEntity?

    @Query("SELECT * FROM notebooks WHERE name = :name AND id != :excludeId")
    suspend fun getNotebookByNameExcludingId(name: String, excludeId: Long): NotebookEntity?

    @Upsert
    suspend fun upsertNotebook(notebook: NotebookEntity): Long

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteNotebook(id: Long)
}