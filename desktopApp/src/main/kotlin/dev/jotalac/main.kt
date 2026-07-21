package dev.jotalac

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.jotalac.core.di.initKoin

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "git-writer",
        ) {
            App()
        }
    }
}