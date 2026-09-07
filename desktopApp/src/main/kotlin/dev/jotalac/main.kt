package dev.jotalac

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.jotalac.core.di.initFileKitJvm
import dev.jotalac.core.di.initKoin
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension


fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "git-writer",
            undecorated = false,
            icon = painterResource(Res.drawable.app_logo),

            ) {

            val minWindowDimensions = with(LocalDensity.current) { 600.dp.roundToPx() }
            SideEffect {
                window.minimumSize = Dimension(minWindowDimensions, minWindowDimensions)
            }

            initFileKitJvm("git-writer")

            App()
        }
    }
}