package dev.jotalac.core.ui.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.LocalTextStyle

/**
 * A screen - the [topBar] above the content, the [bottomBar] below it, the floating button and the snackbars over
 * it. The content gets the padding of the system bars at the bottom and sides - the top bar keeps out of the status
 * bar itself.
 */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = AppTheme.colors.background,
    content: @Composable (PaddingValues) -> Unit,
) {
    val contentPadding = WindowInsets.systemBars
        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        .asPaddingValues()

    Surface(color = containerColor, modifier = modifier.fillMaxSize()) {
        Box {
            Column {
                topBar()
                Box(Modifier.weight(1f)) { content(contentPadding) }
                bottomBar()
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                floatingButton()
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                snackbarHost()
            }
        }
    }
}

/** A screen's bar at the top - a back button or so in [leading], the [title], and the [actions] at its end - a line below it. */
@Composable
fun TopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = AppTheme.colors.background,
) {
    Surface(color = containerColor, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .height(52.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke()
                Box(Modifier.weight(1f).padding(start = if (leading == null) 12.dp else 4.dp)) {
                    CompositionLocalProvider(LocalTextStyle provides AppTheme.typography.title) {
                        title()
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
            Divider()
        }
    }
}
