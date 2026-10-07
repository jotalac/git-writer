package dev.jotalac.testing.fakes

import dev.jotalac.core.data.UserSettings
import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * user settings without the data store
 */
class FakeUserSettings(
    initial: UserSettingsState = UserSettingsState(),
) : UserSettings {

    val settings = MutableStateFlow(initial)
    override val userSettingsStateFlow: Flow<UserSettingsState> = settings

    val calledWrites = mutableListOf<String>()

    override suspend fun setThemeMode(themeMode: AppThemeMode) { calledWrites += "themeMode" }
    override suspend fun setThemeAccentColor(accentColor: AppThemeAccentColor) { calledWrites += "accentColor" }
    override suspend fun setFont(font: AppFontFamily) { calledWrites += "font" }
    override suspend fun setLanguage(language: AppLanguage) { calledWrites += "language" }
    override suspend fun setGitConflictStrategy(strategy: GitConflictResolutionStrategy) { calledWrites += "conflictStrategy" }
    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) { calledWrites += "dynamicColor" }
}
