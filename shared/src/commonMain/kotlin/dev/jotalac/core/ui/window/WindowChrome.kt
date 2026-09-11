package dev.jotalac.core.ui.window

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val APP_WINDOW_TITLE = "git-writer"

@Immutable
interface WindowChrome {
    /** true when the app draws/replaces the window title bar. */
    val isIntegrated: Boolean

    /** empty space reserved at the start of the strip (macOS traffic lights). */
    val startInset: Dp

    /** height of the app-drawn title strip. */
    val titleBarHeight: Dp

    /** keeps the OS window/taskbar title in sync with the screen. */
    fun setTitle(title: String)

    /**
     * sets the tile bar to dark on windows
     */
    fun setTitleBarDark(isDark: Boolean)
}

private object InactiveWindowChrome : WindowChrome {
    override val isIntegrated: Boolean = false
    override val startInset: Dp = 0.dp
    override val titleBarHeight: Dp = 0.dp

    override fun setTitle(title: String) = Unit
    override fun setTitleBarDark(isDark: Boolean) = Unit
}

val LocalWindowChrome = staticCompositionLocalOf<WindowChrome> { InactiveWindowChrome }
