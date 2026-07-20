package dev.jotalac

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.jotalac.core.navigation.Route
import dev.jotalac.core.ui.components.CustomScaffold
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.feature.editor.ui.EditorScreen

@Composable
@Preview
fun App() {
    AppTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Route.MainApp) {
            composable<Route.MainApp> {
                
                EditorScreen()
            }
        }
    }
}
