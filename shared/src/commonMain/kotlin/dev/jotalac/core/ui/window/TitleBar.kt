package dev.jotalac.core.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import dev.jotalac.core.ui.design.TopBar

/*
single topAppBar for all platforms
- mobile renders the app's TopBar
- desktop has custom top app
 */
@Composable
internal fun TitleBar(
    title: @Composable () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val chrome = LocalWindowChrome.current
    val host = LocalTitleBarHost.current

    if (chrome.isIntegrated && host != null) {
        SideEffect { host.publish(leading = leading, title = title, actions = actions) }
    } else {
        TopBar(
            leading = leading,
            title = title,
            actions = { actions?.invoke() },
        )
    }
}
