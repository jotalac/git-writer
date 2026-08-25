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
import dev.jotalac.core.domain.GitConflictResolutionStrategy
import dev.jotalac.feature.settings.ui.SectionTitle
import dev.jotalac.feature.settings.ui.SettingsCollapsableSection
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettings(
    selectedStrategy: GitConflictResolutionStrategy,
    onStrategyChange: (GitConflictResolutionStrategy) -> Unit,
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

        SettingsCollapsableSection(
            title = stringResource(Res.string.settings_sync_section_title),
            subtitle = stringResource(Res.string.settings_sync_section_subtitle),
            initiallyExpanded = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.settings_conflict_strategy_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = selectedStrategy == GitConflictResolutionStrategy.MANUAL,
                            onClick = { onStrategyChange(GitConflictResolutionStrategy.MANUAL) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = 0,
                                count = 2
                            )
                        ) {
                            Text(stringResource(Res.string.settings_conflict_manual))
                        }

                        SegmentedButton(
                            selected = selectedStrategy == GitConflictResolutionStrategy.LOCAL || selectedStrategy == GitConflictResolutionStrategy.REMOTE,
                            onClick = {
                                if (selectedStrategy == GitConflictResolutionStrategy.MANUAL) {
                                    onStrategyChange(GitConflictResolutionStrategy.LOCAL)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = 1,
                                count = 2
                            )
                        ) {
                            Text(stringResource(Res.string.settings_conflict_auto))
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedStrategy != GitConflictResolutionStrategy.MANUAL,
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
                            // option: always accept local
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Option: always accept remote
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
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
