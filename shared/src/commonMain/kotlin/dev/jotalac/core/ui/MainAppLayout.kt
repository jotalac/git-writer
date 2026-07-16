package dev.jotalac.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlexDirection.Companion.Row
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MainLayoutScreen(
    currentScreen: @Composable () -> Unit
) {
    // 1. The Scaffold wraps the entire window
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("main-branch / my-note.md") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Commit to Git */ }) {
                Text("Sync")
            }
        }
    ) { paddingValues ->

        // 2. Inside the Scaffold, we split the screen horizontally for Desktop
//        Row(
//            modifier = Modifier.padding(paddingValues)
//        ) {
//            // 3. The Left Sidebar (File tree, branches, settings)
//            NavigationRail {
//                NavigationRailItem(
//                    selected = true,
//                    onClick = { },
//                    icon = { Text("📝") },
//                    label = { Text("Editor") }
//                )
//                NavigationRailItem(
//                    selected = false,
//                    onClick = { },
//                    icon = { Text("⚙️") },
//                    label = { Text("Settings") }
//                )
//            }

//        }
        Box(modifier = Modifier.padding(paddingValues)) {
            currentScreen()

        }
    }
}