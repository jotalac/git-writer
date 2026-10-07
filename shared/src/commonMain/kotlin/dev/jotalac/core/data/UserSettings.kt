package dev.jotalac.core.data

import dev.jotalac.core.domain.*
import kotlinx.coroutines.flow.Flow

/**
 * What the app reads and writes as user settings.
 *
 * [UserSettingsManager] is the real, DataStore-backed implementation; this exists so ViewModels can
 * depend on the behaviour without needing a DataStore on the test classpath.
 */
interface UserSettings {
    val userSettingsStateFlow: Flow<UserSettingsState>

    suspend fun setThemeMode(themeMode: AppThemeMode)
    suspend fun setThemeAccentColor(accentColor: AppThemeAccentColor)
    suspend fun setFont(font: AppFontFamily)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setGitConflictStrategy(strategy: GitConflictResolutionStrategy)
    suspend fun setUseDynamicColor(useDynamicColor: Boolean)
}
