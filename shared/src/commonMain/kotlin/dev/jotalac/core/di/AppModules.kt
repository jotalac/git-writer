package dev.jotalac.core.di

import dev.jotalac.feature.editor.ui.EditorViewModel
import dev.jotalac.feature.notebooks_management.ui.NotebookManagementViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// platform specific module
expect val platformModule: Module

val featureModules = module {
    viewModelOf(::EditorViewModel)
    viewModelOf(::NotebookManagementViewModel)
}

val appModules = listOf(featureModules, platformModule)

fun initKoin() {
    startKoin {
        modules(appModules)
    }
}