package dev.jotalac.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.data.UserSettings
import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.domain.*
import dev.jotalac.feature.app_update.data.AppUpdateRepository
import dev.jotalac.feature.app_update.domain.LatestRelease
import dev.jotalac.feature.app_update.domain.isNewerVersion
import git_writer.shared.BuildKonfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUpdateUiState(
    val installedVersion: String = BuildKonfig.APP_VERSION,
    val latestRelease: LatestRelease? = null,
    val isChecking: Boolean = false,
    val checkFailed: Boolean = false,
) {
    val isUpdateAvailable: Boolean
        get() = latestRelease != null && isNewerVersion(latestRelease.version, installedVersion)
}

class SettingsViewModel(
    private val userSettingsManager: UserSettings,
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {

    val userSettingsState = userSettingsManager.userSettingsStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettingsState()
        )

    // the result of a check started from settings
    private val manualCheck = MutableStateFlow(AppUpdateUiState())

    val appUpdateState = combine(appUpdateRepository.state, manualCheck) { stored, check ->
        check.copy(latestRelease = stored.latestRelease)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUpdateUiState()
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
                is SettingsAction.ChangeEditorFullWidth ->
                    userSettingsManager.setEditorFullWidth(action.editorFullWidth)

                is SettingsAction.ChangeLanguage ->
                    userSettingsManager.setLanguage(action.language)

                is SettingsAction.ChangeSpellcheckLanguage ->
                    userSettingsManager.setSpellcheckLanguage(action.language)

                is SettingsAction.ChangeGitConflictStrategy ->
                    userSettingsManager.setGitConflictStrategy(action.strategy)

                is SettingsAction.ChangeUseDynamicColor ->
                    userSettingsManager.setUseDynamicColor(action.useDynamicColor)

                is SettingsAction.ChangeAutoSync ->
                    userSettingsManager.setAutoSync(action.autoSync)

                SettingsAction.CheckForUpdates -> checkForUpdates()
            }
        }
    }

    private suspend fun checkForUpdates() {
        if (manualCheck.value.isChecking) return
        manualCheck.update { it.copy(isChecking = true, checkFailed = false) }
        val failed = appUpdateRepository.checkNow().isFailure
        manualCheck.update { it.copy(isChecking = false, checkFailed = failed) }
    }


    sealed interface SettingsAction {
        data class ChangeThemeMode(val themeMode: AppThemeMode) : SettingsAction
        data class ChangeThemeAccentColor(val accentColor: AppThemeAccentColor) : SettingsAction
        data class ChangeFont(val font: AppFontFamily) : SettingsAction
        data class ChangeEditorFullWidth(val editorFullWidth: Boolean) : SettingsAction
        data class ChangeLanguage(val language: AppLanguage) : SettingsAction
        data class ChangeSpellcheckLanguage(val language: SpellcheckLanguage) : SettingsAction
        data class ChangeGitConflictStrategy(val strategy: GitConflictResolutionStrategy) : SettingsAction
        data class ChangeUseDynamicColor(val useDynamicColor: Boolean) : SettingsAction
        data class ChangeAutoSync(val autoSync: Boolean) : SettingsAction
        data object CheckForUpdates : SettingsAction
    }
}
