package dev.jotalac.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appDir = File(FileKit.filesDir.path)

    if (!appDir.exists()) {
        appDir.mkdirs()
    }

    val dbFile = File(appDir.path, "app_db.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    )
}