package dev.jotalac.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.ui.design.AppDialog
import dev.jotalac.core.ui.design.DialogSize
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.settings
import git_writer.shared.generated.resources.settings_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The settings on desktop - [SettingsScreen] on mobile. */
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val settingsState by settingsViewModel.userSettingsState.collectAsStateWithLifecycle()

    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_title),
        icon = painterResource(Res.drawable.settings),
        size = DialogSize.Large,
        showCloseButton = true,
    ) {
        SettingsContent(
            userSettingsState = settingsState,
            onAction = settingsViewModel::onAction,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}
