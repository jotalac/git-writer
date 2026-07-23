package dev.jotalac.core.di

import androidx.room.RoomDatabase
import dev.jotalac.core.database.AppDatabase
import dev.jotalac.core.database.getDatabaseBuilder
import dev.jotalac.feature.notebooks_management.domain.AndroidNotebookPathProvider
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<NotebookPathProvider> { AndroidNotebookPathProvider(context = androidContext()) }

    single<RoomDatabase.Builder<AppDatabase>> {
        getDatabaseBuilder(androidContext())
    }
}