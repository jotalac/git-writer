package dev.jotalac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.data.UserSettings
import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.feature.app_update.data.AppUpdateRepository
import git_writer.shared.BuildKonfig
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.msg_update_available
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

class AppViewModel(
    userSettingsManager: UserSettings,
    appUpdateRepository: AppUpdateRepository,
    snackbarManager: SnackbarManager,
) : ViewModel() {
    val userSettingsState = userSettingsManager.userSettingsStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettingsState()
        )

    init {
        // check once a day if newer version isn't available from releases
        viewModelScope.launch {
            appUpdateRepository.checkIfDue(BuildKonfig.APP_VERSION)?.let { release ->
                // a snackbar sent before the editor shows them would be lost - at startup it is not there yet
                snackbarManager.messages.subscriptionCount.first { it > 0 }
                snackbarManager.showMessage(
                    SnackbarText.resource(Res.string.msg_update_available, BuildKonfig.APP_VERSION, release.version)
                )
            }
        }
    }
}
