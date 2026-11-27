package dev.jotalac.feature.settings.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.core.ui.components.DelayedTooltip
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.feature.settings.ui.settings_sections.AboutSettings
import dev.jotalac.feature.settings.ui.settings_sections.AppearanceSettings
import dev.jotalac.feature.settings.ui.settings_sections.LanguageSettings
import dev.jotalac.feature.settings.ui.settings_sections.SyncSettings
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.arrow_right
import git_writer.shared.generated.resources.github
import git_writer.shared.generated.resources.opened_book
import git_writer.shared.generated.resources.settings_docs_link
import git_writer.shared.generated.resources.settings_github_link
import git_writer.shared.generated.resources.toggle_collapse
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsContent(
    userSettingsState: UserSettingsState,
    appUpdateState: AppUpdateUiState,
    onAction: (SettingsViewModel.SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AppearanceSettings(
                selectedTheme = userSettingsState.themeMode,
                selectedThemeAccentColor = userSettingsState.themeAccentColor,
                selectedFontFamily = userSettingsState.font,
                onThemeChange = { onAction(SettingsViewModel.SettingsAction.ChangeThemeMode(it)) },
                onThemeAccentColorChange = { onAction(SettingsViewModel.SettingsAction.ChangeThemeAccentColor(it)) },
                onFontFamilyChange = { onAction(SettingsViewModel.SettingsAction.ChangeFont(it)) },
                useDynamicColor = userSettingsState.useDynamicColor,
                onDynamicColorToggle = { onAction(SettingsViewModel.SettingsAction.ChangeUseDynamicColor(it)) },
                editorFullWidth = userSettingsState.editorFullWidth,
                onEditorFullWidthChange = { onAction(SettingsViewModel.SettingsAction.ChangeEditorFullWidth(it)) }
            )

            LanguageSettings(
                selectedLanguage = userSettingsState.language,
                onLanguageChange = { onAction(SettingsViewModel.SettingsAction.ChangeLanguage(it)) },
                selectedSpellcheckLanguage = userSettingsState.spellcheckLanguage,
                onSpellcheckLanguageChange = {
                    onAction(SettingsViewModel.SettingsAction.ChangeSpellcheckLanguage(it))
                }
            )

            SyncSettings(
                selectedStrategy = userSettingsState.gitConflictStrategy,
                onStrategyChange = { onAction(SettingsViewModel.SettingsAction.ChangeGitConflictStrategy(it)) },
                autoSync = userSettingsState.autoSync,
                onAutoSyncChange = { onAction(SettingsViewModel.SettingsAction.ChangeAutoSync(it)) },
            )

            AboutSettings(
                state = appUpdateState,
                onCheckForUpdates = { onAction(SettingsViewModel.SettingsAction.CheckForUpdates) },
            )

            ProjectLinks(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        AppVerticalScrollbar(
            listState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}

private const val GITHUB_URL = "https://github.com/jotalac/git-writer"
private const val DOCS_URL = "https://jotalac.github.io/git-writer-docs/"

// links to the project's repository and its documentation, opened in the browser
@Composable
private fun ProjectLinks(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProjectLink(Res.drawable.github, stringResource(Res.string.settings_github_link), GITHUB_URL)
            ProjectLink(Res.drawable.opened_book, stringResource(Res.string.settings_docs_link), DOCS_URL)
        }

        Text(
            text = "Free and Open-source",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

    }
}

@Composable
private fun ProjectLink(icon: DrawableResource, label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    DelayedTooltip(text = label) {
        IconButton(onClick = { uriHandler.openUri(url) }) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(MaterialTheme.dimensions.iconLarge),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    icon: DrawableResource,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier.size(MaterialTheme.dimensions.iconLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

    }
}

@Composable
fun SettingsCollapsableSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    // draws attention to the section: shown next to the title, with the subtitle in the accent color
    highlightIcon: DrawableResource? = null,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (highlightIcon != null) {
                            Icon(
                                painter = painterResource(highlightIcon),
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highlightIcon != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    painter = painterResource(Res.drawable.arrow_right),
                    contentDescription = stringResource(Res.string.toggle_collapse),
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.iconSmall)
                        .graphicsLayer { rotationZ = rotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    content()
                }
            }
        }
    }
}