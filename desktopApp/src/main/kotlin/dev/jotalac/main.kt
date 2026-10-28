package dev.jotalac

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import dev.jotalac.core.di.runAppExitTasks
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.jotalac.core.utils.isMacOsPlatform
import dev.jotalac.feature.git_sync.data.jgitBypassUrlRewrite
import dev.jotalac.window.DesktopWindowChrome
import dev.jotalac.window.applyMacOsUnifiedTitleBar
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource

private val MinWindowSize = DpSize(650.dp, 650.dp)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // make the macos dialog the correct color theme
    System.setProperty("apple.awt.application.appearance", "system")

    initKoin()
    jgitBypassUrlRewrite()

    application {
        val scope = rememberCoroutineScope()
        var isClosing by remember { mutableStateOf(false) }

        Window(
            // the window disappears at once, the app exits after its last sync (auto sync) - at most 15 s
            onCloseRequest = {
                if (!isClosing) {
                    isClosing = true
                    scope.launch {
                        withTimeoutOrNull(15.seconds) { runAppExitTasks() }
                        exitApplication()
                    }
                }
            },
            visible = !isClosing,
            state = rememberWindowState(),
            title = APP_WINDOW_TITLE,
            icon = painterResource(Res.drawable.app_logo),
            minSize = MinWindowSize,
        ) {
            val windowChrome = remember(window) {
                DesktopWindowChrome(window = window)
            }

            if (isMacOsPlatform) {
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
