package dev.jotalac.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.data.UserSettingsManager
import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.domain.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userSettingsManager: UserSettingsManager
) : ViewModel() {

    val userSettingsState = userSettingsManager.userSettingsStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettingsState()
        )

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                is SettingsAction.ChangeThemeMode ->
                    userSettingsManager.setThemeMode(action.themeMode)

                is SettingsAction.ChangeThemeAccentColor ->
                    userSettingsManager.setThemeAccentColor(action.accentColor)

                is SettingsAction.ChangeFont ->
                    userSettingsManager.setFont(action.font)

                is SettingsAction.ChangeLanguage ->
                    userSettingsManager.setLanguage(action.language)

                is SettingsAction.ChangeGitConflictStrategy ->
                    userSettingsManager.setGitConflictStrategy(action.strategy)
            }
        }
    }


    sealed interface SettingsAction {
        data class ChangeThemeMode(val themeMode: AppThemeMode) : SettingsAction
        data class ChangeThemeAccentColor(val accentColor: AppThemeAccentColor) : SettingsAction
        data class ChangeFont(val font: AppFontFamily) : SettingsAction
        data class ChangeLanguage(val language: AppLanguage) : SettingsAction
        data class ChangeGitConflictStrategy(val strategy: GitConflictResolutionStrategy) : SettingsAction
    }
}