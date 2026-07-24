package dev.jotalac.feature.notebooks_management.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val directoryPath: String,
    val remoteUrl: String?,
    val remoteUsername: String?,
    val remotePassword: String?,
)