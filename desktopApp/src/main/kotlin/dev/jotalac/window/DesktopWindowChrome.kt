package dev.jotalac.window

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.window.WindowChrome
import dev.jotalac.core.utils.isMacOsPlatform

/**
 * control calls for the desktop app window
 * owns the state of
 */
internal class DesktopWindowChrome(
    private val window: ComposeWindow,
) : WindowChrome {

    override val isIntegrated: Boolean = isMacOsPlatform

    override val startInset = if (isMacOsPlatform) MACOS_TRAFFIC_LIGHTS_INSET else 0.dp

    override val titleBarHeight = 56.dp

    override fun setTitle(title: String) {
        if (window.title != title) {
            window.title = title
        }
    }

    override fun setTitleBarDark(isDark: Boolean) {
        window.applyWindowsTitleBarTheme(isDark)
    }

    private companion object {
        // space the macOS traffic lights occupy at the start of the strip.
        val MACOS_TRAFFIC_LIGHTS_INSET = 75.dp
    }
}
