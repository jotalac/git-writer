package dev.jotalac.feature.settings.ui.settings_sections

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.settings.ui.SectionTitle
import dev.jotalac.feature.settings.ui.SettingsCollapsableSection
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

enum class ConflictResolutionMode {
    MANUAL,
    AUTO
}

enum class AutoMergePreference {
    ACCEPT_LOCAL,
    ACCEPT_REMOTE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettings() {
    var resolutionMode by remember { mutableStateOf(ConflictResolutionMode.MANUAL) }
    var autoMergePreference by remember { mutableStateOf(AutoMergePreference.ACCEPT_LOCAL) }

    val resolutionOptions = listOf(
        ConflictResolutionMode.MANUAL to stringResource(Res.string.settings_conflict_manual),
        ConflictResolutionMode.AUTO to stringResource(Res.string.settings_conflict_auto)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            stringResource(Res.string.settings_sync_title),
            Res.drawable.git_merge
        )

        SettingsCollapsableSection(
            title = stringResource(Res.string.settings_sync_section_title),
            subtitle = stringResource(Res.string.settings_sync_section_subtitle),
            initiallyExpanded = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. Conflict Resolution Mode (Manual vs Auto)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.settings_conflict_strategy_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        resolutionOptions.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = resolutionMode == mode,
                                onClick = { resolutionMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = resolutionOptions.size
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }
                }

                // 2. Auto Merge Sub-options (visible when AUTO is selected)
                AnimatedVisibility(
                    visible = resolutionMode == ConflictResolutionMode.AUTO,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = stringResource(Res.string.settings_auto_merge_preference_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Option: Always accept local
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { autoMergePreference = AutoMergePreference.ACCEPT_LOCAL }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = autoMergePreference == AutoMergePreference.ACCEPT_LOCAL,
                                    onClick = { autoMergePreference = AutoMergePreference.ACCEPT_LOCAL }
                                )
                                Text(
                                    text = stringResource(Res.string.settings_auto_accept_local),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Option: Always accept remote
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { autoMergePreference = AutoMergePreference.ACCEPT_REMOTE }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = autoMergePreference == AutoMergePreference.ACCEPT_REMOTE,
                                    onClick = { autoMergePreference = AutoMergePreference.ACCEPT_REMOTE }
                                )
                                Text(
                                    text = stringResource(Res.string.settings_auto_accept_remote),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
