package dev.jotalac

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.jotalac.core.di.initFileKitJvm
import dev.jotalac.core.di.initKoin
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource


fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "git-writer",
            undecorated = false,
            icon = painterResource(Res.drawable.app_logo)
        ) {

            initFileKitJvm("git-writer")

            App()
        }
    }
}