package dev.jotalac.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                CustomSnackbar(snackbarData)
            }
        },
        floatingActionButton = floatingActionButton,
//        floatingActionButton = {
//            FloatingActionButton(onClick = { /* Commit to Git */ }) {
//                Text("Sync")
//            }
//        }
    ) { paddingValues ->
        content(paddingValues)
    }
}