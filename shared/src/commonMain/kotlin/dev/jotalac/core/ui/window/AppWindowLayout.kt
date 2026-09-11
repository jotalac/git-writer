package dev.jotalac.core.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * whole app window - includes the top bar for desktop and the app content
 * for mobile it simply puts in the content
 */
@Composable
internal fun AppWindowLayout(
    titleBarHost: TitleBarHost,
    content: @Composable () -> Unit,
) {
    if (LocalWindowChrome.current.isIntegrated) {
        Column(modifier = Modifier.fillMaxSize()) {
            WindowTitleBar(titleBarHost)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    } else {
        content()
    }
}
