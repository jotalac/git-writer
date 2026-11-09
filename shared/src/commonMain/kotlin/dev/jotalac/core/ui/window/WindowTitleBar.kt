package dev.jotalac.core.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.design.Divider
import dev.jotalac.core.ui.design.Surface
import dev.jotalac.core.ui.theme.AppTheme

/**
 * the desktop window title strip - with the draggable window handle and buttons
 */
@Composable
internal fun WindowTitleBar(host: TitleBarHost) {
    val chrome = LocalWindowChrome.current

    Surface(
        // A tone above the plain content background so the strip reads as the app's title bar.
        color = AppTheme.colors.surface,
        contentColor = AppTheme.colors.text,
        modifier = Modifier
            .fillMaxWidth()
            .draggableWindow(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chrome.titleBarHeight)
                    .padding(start = chrome.startInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                host.leading?.invoke()
                Box(modifier = Modifier.weight(1f)) {
                    host.title?.invoke()
                }
                host.actions?.invoke()
            }

            Divider(
                thickness = 1.dp,
                color = AppTheme.colors.divider.copy(alpha = 0.5f),
            )
        }
    }
}
