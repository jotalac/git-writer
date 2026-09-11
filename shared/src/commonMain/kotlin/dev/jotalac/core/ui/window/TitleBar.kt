package dev.jotalac.core.ui.window

import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

/*
single topAppBar for all platforms
- mobile renders the material3 TopAppBar
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
        TopAppBar(
            navigationIcon = { leading?.invoke() },
            title = { title() },
            actions = { actions?.invoke() },
        )
    }
}
