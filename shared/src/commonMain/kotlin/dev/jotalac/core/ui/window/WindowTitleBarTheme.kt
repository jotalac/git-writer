package dev.jotalac.core.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * keep the native OS title bar in synch with app theme on windows
 */
@Composable
internal fun WindowTitleBarTheme(isDark: Boolean) {
    val chrome = LocalWindowChrome.current

    LaunchedEffect(isDark, chrome) {
        chrome.setTitleBarDark(isDark)
    }
}
