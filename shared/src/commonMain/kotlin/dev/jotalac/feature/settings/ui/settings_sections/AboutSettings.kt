package dev.jotalac.feature.settings.ui.settings_sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.feature.settings.ui.AppUpdateUiState
import dev.jotalac.feature.settings.ui.SectionTitle
import dev.jotalac.feature.settings.ui.SettingsCollapsableSection
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// the installed and the latest version - the update is downloaded from the release page
@Composable
fun AboutSettings(
    state: AppUpdateUiState,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val latestRelease = state.latestRelease

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(stringResource(Res.string.settings_about_title), Res.drawable.info_icon)

        SettingsCollapsableSection(
            title = stringResource(Res.string.settings_app_version_title),
            subtitle = when {
                state.isUpdateAvailable -> stringResource(Res.string.settings_update_available)
                latestRelease != null -> stringResource(Res.string.settings_up_to_date)
                else -> null
            },
            initiallyExpanded = true,
            highlightIcon = Res.drawable.download.takeIf { state.isUpdateAvailable },
        ) {
            VersionRow(stringResource(Res.string.settings_installed_version), state.installedVersion)
            VersionRow(
                stringResource(Res.string.settings_latest_version),
                latestRelease?.version ?: stringResource(Res.string.settings_latest_version_unknown)
            )

            if (state.checkFailed) {
                Text(
                    text = stringResource(Res.string.settings_check_updates_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCheckForUpdates, enabled = !state.isChecking) {
                    if (state.isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(Res.string.settings_check_updates))
                }

                if (state.isUpdateAvailable && latestRelease != null) {
                    Button(onClick = { uriHandler.openUri(latestRelease.url) }) {
                        Icon(
                            painter = painterResource(Res.drawable.download),
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimensions.iconMedium)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.settings_download_update, latestRelease.version))
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionRow(label: String, version: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = version,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
