package dev.jotalac.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.ui.components.CustomScaffold
import dev.jotalac.core.ui.components.TopAppBarIcon
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = koinViewModel(),
) {
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val settingsState by settingsViewModel.userSettingsState.collectAsStateWithLifecycle()

    CustomScaffold(
        snackbarHostState = snackbarHostState,
        topAppBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.settings),
                            contentDescription = "close icon",
                        )

                        Text(
                            text = stringResource(Res.string.settings_title),
//                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                actions = {
                    TopAppBarIcon(
                        onClick = onNavigateBack,
                        icon = Res.drawable.x_icon,
                        contentDescription = stringResource(Res.string.toggle_side_bar_desc),
                    )
                },
            )
        }
    ) { innerPadding ->
        SettingsContent(
            userSettingsState = settingsState,
            onAction = settingsViewModel::onAction,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        )
    }

}