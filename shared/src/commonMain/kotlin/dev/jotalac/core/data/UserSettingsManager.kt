package dev.jotalac.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.jotalac.core.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserSettingsState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val themeAccentColor: AppThemeAccentColor = AppThemeAccentColor.DEFAULT,
    val font: AppFontFamily = AppFontFamily.DEFAULT,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val gitConflictStrategy: GitConflictResolutionStrategy = GitConflictResolutionStrategy.MANUAL,
    val useDynamicColor: Boolean = false,
)

class UserSettingsManager(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val THEME_ACCENT_COLOR_KEY = stringPreferencesKey("theme_accent_color")
        private val FONT_FAMILY_KEY = stringPreferencesKey("font_family")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val CONFLICT_STRATEGY_KEY = stringPreferencesKey("conflict_strategy")
        private val USE_DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    }

    val userSettingsStateFlow: Flow<UserSettingsState> = dataStore.data
        .map { preferences ->
            UserSettingsState(
                themeMode = preferences[THEME_MODE_KEY]?.let { name ->
                    AppThemeMode.entries.find { it.name == name }
                } ?: AppThemeMode.SYSTEM,
                font = preferences[FONT_FAMILY_KEY]?.let { name ->
                    AppFontFamily.entries.find { it.name == name }
                } ?: AppFontFamily.DEFAULT,
                themeAccentColor = preferences[THEME_ACCENT_COLOR_KEY]?.let { name ->
                    AppThemeAccentColor.entries.find { it.name == name }
                } ?: AppThemeAccentColor.DEFAULT,
                language = preferences[LANGUAGE_KEY]?.let { name ->
                    AppLanguage.entries.find { it.name == name }
                } ?: AppLanguage.ENGLISH,
                gitConflictStrategy = preferences[CONFLICT_STRATEGY_KEY]?.let { name ->
                    GitConflictResolutionStrategy.entries.find { it.name == name }
                } ?: GitConflictResolutionStrategy.MANUAL,
                useDynamicColor = preferences[USE_DYNAMIC_COLOR_KEY] ?: false,
            )
        }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }

    suspend fun setThemeAccentColor(accentColor: AppThemeAccentColor) {
        dataStore.edit { preferences ->
            preferences[THEME_ACCENT_COLOR_KEY] = accentColor.name
        }
    }

    suspend fun setFont(font: AppFontFamily) {
        dataStore.edit { preferences ->
            preferences[FONT_FAMILY_KEY] = font.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }

    suspend fun setGitConflictStrategy(strategy: GitConflictResolutionStrategy) {
        dataStore.edit { preferences ->
            preferences[CONFLICT_STRATEGY_KEY] = strategy.name
        }
    }

    suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLOR_KEY] = useDynamicColor
        }
    }

    suspend fun clearSettings() {
        dataStore.edit { preferences ->
            preferences.remove(THEME_MODE_KEY)
            preferences.remove(THEME_ACCENT_COLOR_KEY)
            preferences.remove(FONT_FAMILY_KEY)
            preferences.remove(LANGUAGE_KEY)
            preferences.remove(CONFLICT_STRATEGY_KEY)
        }
    }
}