package dev.jotalac.feature.settings.ui.settings_sections

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.jotalac.core.domain.GitConflictResolutionStrategy
import dev.jotalac.core.ui.design.*
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.feature.settings.ui.SectionTitle
import dev.jotalac.feature.settings.ui.SettingsCollapsableSection
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun SyncSettings(
    selectedStrategy: GitConflictResolutionStrategy,
    onStrategyChange: (GitConflictResolutionStrategy) -> Unit,
    autoSync: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            stringResource(Res.string.settings_sync_title),
            Res.drawable.git_merge
        )

        AutoSyncSettings(autoSync = autoSync, onAutoSyncChange = onAutoSyncChange)

        SettingsCollapsableSection(
            title = stringResource(Res.string.settings_sync_section_title),
            subtitle = stringResource(Res.string.settings_sync_section_subtitle),
            initiallyExpanded = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.settings_conflict_strategy_label),
                        style = AppTheme.typography.label,
                        color = AppTheme.colors.textSecondary
                    )

                    // manual, or automatic - which side wins is chosen below
                    SegmentedControl(
                        options = listOf(false, true),
                        selected = selectedStrategy != GitConflictResolutionStrategy.MANUAL,
                        onSelect = { isAutomatic ->
                            when {
                                !isAutomatic -> onStrategyChange(GitConflictResolutionStrategy.MANUAL)
                                selectedStrategy == GitConflictResolutionStrategy.MANUAL ->
                                    onStrategyChange(GitConflictResolutionStrategy.LOCAL)
                            }
                        },
                        label = { isAutomatic ->
                            stringResource(if (isAutomatic) Res.string.settings_conflict_auto else Res.string.settings_conflict_manual)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AnimatedVisibility(
                    visible = selectedStrategy != GitConflictResolutionStrategy.MANUAL,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Divider(
                            color = AppTheme.colors.divider.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = stringResource(Res.string.settings_auto_merge_preference_label),
                            style = AppTheme.typography.label,
                            color = AppTheme.colors.textSecondary
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // option: always accept local
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppTheme.shapes.small)
                                    .clickable { onStrategyChange(GitConflictResolutionStrategy.LOCAL) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedStrategy == GitConflictResolutionStrategy.LOCAL,
                                    onClick = { onStrategyChange(GitConflictResolutionStrategy.LOCAL) }
                                )
                                Text(
                                    text = stringResource(Res.string.settings_auto_accept_local),
                                    style = AppTheme.typography.body,
                                    color = AppTheme.colors.text
                                )
                            }

                            // Option: always accept remote
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppTheme.shapes.small)
                                    .clickable { onStrategyChange(GitConflictResolutionStrategy.REMOTE) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedStrategy == GitConflictResolutionStrategy.REMOTE,
                                    onClick = { onStrategyChange(GitConflictResolutionStrategy.REMOTE) }
                                )
                                Text(
                                    text = stringResource(Res.string.settings_auto_accept_remote),
                                    style = AppTheme.typography.body,
                                    color = AppTheme.colors.text
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoSyncSettings(
    autoSync: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
) {
    SettingsCollapsableSection(
        title = stringResource(Res.string.settings_auto_sync_title),
        subtitle = stringResource(Res.string.settings_auto_sync_subtitle),
        initiallyExpanded = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.settings_auto_sync_label),
                style = AppTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = autoSync,
                onCheckedChange = onAutoSyncChange
            )
        }
    }
}
