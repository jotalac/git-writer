package dev.jotalac.core.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * keeps the OS window title in synch with whats on the screen
 */
@Composable
internal fun WindowTitle(title: String?) {
    val chrome = LocalWindowChrome.current

    LaunchedEffect(title, chrome) {
        chrome.setTitle(title?.takeIf { it.isNotBlank() } ?: APP_WINDOW_TITLE)
    }
}
