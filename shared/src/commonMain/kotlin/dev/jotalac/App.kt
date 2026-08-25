package dev.jotalac

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.jotalac.core.domain.AppThemeMode
import dev.jotalac.core.navigation.Route
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.ConfigureAppImageLoader
import dev.jotalac.core.utils.applyAppLanguage
import dev.jotalac.feature.editor.ui.EditorScreen
import dev.jotalac.feature.settings.ui.SettingsScreen
import org.koin.compose.koinInject

@Composable
fun App(
    appViewModel: AppViewModel = koinInject(),
) {
    ConfigureAppImageLoader()


    val settingsState by appViewModel.userSettingsState.collectAsStateWithLifecycle()
    val isDarkTheme = when (settingsState.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    applyAppLanguage(settingsState.language)

    key(settingsState.language) {
        AppTheme(
            darkTheme = isDarkTheme,
            accentColor = settingsState.themeAccentColor,
            fontFamily = settingsState.font
        ) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Route.MainApp) {
                composable<Route.MainApp> {
                    EditorScreen(
                        openSettingsOnMobile = { navController.navigate(Route.Settings) }
                    )
                }

                composable<Route.Settings> {
                    SettingsScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}
