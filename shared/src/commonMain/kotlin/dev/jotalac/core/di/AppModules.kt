package dev.jotalac.core.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.jotalac.core.database.ActiveNotebookManager
import dev.jotalac.core.database.AppDatabase
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.editor.data.EditorRepositoryImpl
import dev.jotalac.feature.editor.domain.EditorRepository
import dev.jotalac.feature.editor.ui.EditorViewModel
import dev.jotalac.feature.editor_sidebar.ui.EditorSidebarViewModel
import dev.jotalac.feature.notebooks_management.data.NotebookRepositoryImpl
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import dev.jotalac.feature.notebooks_management.ui.create_notebook.CreateNotebookViewModel
import dev.jotalac.feature.notebooks_management.ui.list_notebooks.NotebookListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// platform specific module
expect val platformModule: Module

val coreModule = module {
    includes(platformModule)

    //local database
    single {
        get<RoomDatabase.Builder<AppDatabase>>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single { get<AppDatabase>().getNotebooksDao() }

    single { SnackbarManager() }

    single { ActiveNotebookManager(dataStore = get()) }

}

val featureModules = module {
    //notebook management
    single<NotebookRepository> {
        NotebookRepositoryImpl(notebookDao = get(), activeNotebookManager = get())
    }
    //files management
    single<EditorRepository> {
        EditorRepositoryImpl()
    }

    viewModelOf(::EditorViewModel)
    viewModelOf(::CreateNotebookViewModel)
    viewModelOf(::NotebookListViewModel)
    viewModelOf(::EditorSidebarViewModel)
}

val appModules = listOf(coreModule, featureModules)

fun initKoin() {
    startKoin {
        modules(appModules)
    }
}