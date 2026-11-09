package dev.jotalac.core.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

/**
 * keep the native window in sync with the app theme - the title bar on windows,
 * and the window background, which shows while resizing before the app redraws
 */
@Composable
internal fun WindowTitleBarTheme(isDark: Boolean) {
    val chrome = LocalWindowChrome.current

    LaunchedEffect(isDark, chrome) {
        chrome.setTitleBarDark(isDark)
    }

    val background = MaterialTheme.colorScheme.background
    LaunchedEffect(background, chrome) {
        chrome.setBackground(background)
    }
}
