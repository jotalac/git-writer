package dev.jotalac.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dev.jotalac.feature.notebooks_management.data.NotebookDao
import dev.jotalac.feature.notebooks_management.data.NotebookEntity

@Database(
    entities = [
        NotebookEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getNotebooksDao(): NotebookDao
}

//@Suppress("KotlinNoActualForExpect")
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
