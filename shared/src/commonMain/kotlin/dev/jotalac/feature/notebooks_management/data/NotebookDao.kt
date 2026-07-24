package dev.jotalac.feature.notebooks_management.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import dev.jotalac.feature.notebooks_management.domain.Notebook
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {

    @Query("SELECT * FROM notebooks ORDER BY id DESC")
    fun getNotebooksAsFlow(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    fun getNotebookByIdAsFlow(id: Long): Flow<NotebookEntity?>


    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getNotebookById(id: Long): NotebookEntity?

    @Upsert
    suspend fun upsertNotebook(notebook: NotebookEntity): Long

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteNotebook(id: Long)
}