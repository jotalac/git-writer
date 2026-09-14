package dev.jotalac.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import dev.jotalac.core.data.DATA_STORE_FILE_NAME
import dev.jotalac.core.data.createDataStore
import dev.jotalac.core.database.AppDatabase
import dev.jotalac.core.database.getDatabaseBuilder
import dev.jotalac.feature.git_sync.data.JGitSyncRepositoryImpl
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.notebooks_management.domain.DesktopNotebookPathProvider
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single<NotebookPathProvider> { DesktopNotebookPathProvider() }

    single<RoomDatabase.Builder<AppDatabase>> {
        getDatabaseBuilder()
    }

    single<DataStore<Preferences>> {
        createDataStore {
            val appDataDir = File(FileKit.filesDir.path)

            if (!appDataDir.exists()) {
                appDataDir.mkdirs()
            }

            File(appDataDir, DATA_STORE_FILE_NAME).absolutePath
        }
    }

    single<GitSyncRepository> {
        JGitSyncRepositoryImpl()
    }
}