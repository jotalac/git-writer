package dev.jotalac.core.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf


// holds the current information about the title bar
@Stable
internal class TitleBarHost {
    var leading: (@Composable () -> Unit)? by mutableStateOf(null)
        private set

    var title: (@Composable () -> Unit)? by mutableStateOf(null)
        private set

    var actions: (@Composable () -> Unit)? by mutableStateOf(null)
        private set

    fun publish(
        leading: (@Composable () -> Unit)?,
        title: (@Composable () -> Unit)?,
        actions: (@Composable () -> Unit)?,
    ) {
        this.leading = leading
        this.title = title
        this.actions = actions
    }
}

internal val LocalTitleBarHost = staticCompositionLocalOf<TitleBarHost?> { null }
