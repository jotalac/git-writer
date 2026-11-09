package dev.jotalac.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jotalac.core.ui.design.Scaffold
import dev.jotalac.core.ui.design.SnackbarHost
import dev.jotalac.core.ui.design.SnackbarHostState

@Composable
fun CustomScaffold(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    topAppBar: @Composable () -> Unit = {},
    bottomAppBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topAppBar,
        bottomBar = bottomAppBar,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingButton = floatingActionButton,
        content = content,
    )
}
