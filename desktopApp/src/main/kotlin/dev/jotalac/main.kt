package dev.jotalac

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.v2.Window
import androidx.compose.ui.window.v2.rememberWindowState
import dev.jotalac.core.di.initFileKitJvm
import dev.jotalac.core.di.initKoin
import dev.jotalac.core.ui.window.APP_WINDOW_TITLE
import dev.jotalac.core.ui.window.LocalDesktopAwtWindow
import dev.jotalac.core.ui.window.LocalWindowChrome
import dev.jotalac.window.DesktopWindowChrome
import dev.jotalac.window.applyMacOsUnifiedTitleBar
import dev.jotalac.window.isMacOs
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource

private val MinWindowSize = DpSize(600.dp, 600.dp)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(),
            title = APP_WINDOW_TITLE,
            icon = painterResource(Res.drawable.app_logo),
            minSize = MinWindowSize,
        ) {
            val windowChrome = remember(window) {
                DesktopWindowChrome(window = window)
            }

            if (isMacOs) {
                SideEffect { window.applyMacOsUnifiedTitleBar() }
            }

            initFileKitJvm(APP_WINDOW_TITLE)

            CompositionLocalProvider(
                LocalWindowChrome provides windowChrome,
                LocalDesktopAwtWindow provides window,
            ) {
                App()
            }
        }
    }
}
