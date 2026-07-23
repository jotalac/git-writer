package dev.jotalac.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import dev.jotalac.core.database.AppDatabase
import dev.jotalac.core.database.DATA_STORE_FILE_NAME
import dev.jotalac.core.database.createDataStore
import dev.jotalac.core.database.getDatabaseBuilder
import dev.jotalac.feature.notebooks_management.domain.AndroidNotebookPathProvider
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File


actual val platformModule: Module = module {
    single<NotebookPathProvider> { AndroidNotebookPathProvider(context = androidContext()) }

    single<RoomDatabase.Builder<AppDatabase>> {
        getDatabaseBuilder(androidContext())
    }

    single<DataStore<Preferences>> {
        createDataStore {
            val context = androidContext()
            File(context.filesDir, DATA_STORE_FILE_NAME).absolutePath
        }
    }
}