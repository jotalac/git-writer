package dev.jotalac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.data.UserSettingsManager
import dev.jotalac.core.data.UserSettingsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AppViewModel(
    userSettingsManager: UserSettingsManager
) : ViewModel() {
    val userSettingsState = userSettingsManager.userSettingsStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettingsState()
        )
}