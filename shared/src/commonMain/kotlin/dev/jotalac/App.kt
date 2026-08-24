package dev.jotalac

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.jotalac.core.navigation.Route
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.ConfigureAppImageLoader
import dev.jotalac.feature.editor.ui.EditorScreen
import dev.jotalac.feature.settings.ui.SettingsScreen

@Composable
@Preview
fun App() {
    ConfigureAppImageLoader()

    AppTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Route.MainApp) {
            composable<Route.MainApp> {
                EditorScreen(
                    openSettingsOnMobile = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.Settings> {
                SettingsScreen { navController.popBackStack() }
            }
        }
    }
}
