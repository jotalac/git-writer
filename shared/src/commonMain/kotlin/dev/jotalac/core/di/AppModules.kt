package dev.jotalac.core.di

import dev.jotalac.feature.editor.ui.EditorViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureModules = module {
    viewModelOf(::EditorViewModel)
}

val appModules = listOf(featureModules)

fun initKoin() {
    startKoin {
        modules(appModules)
    }
}