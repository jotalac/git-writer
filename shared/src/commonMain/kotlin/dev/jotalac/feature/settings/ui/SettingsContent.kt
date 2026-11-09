package dev.jotalac.feature.settings.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jotalac.core.data.UserSettingsState
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.core.ui.design.*
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.feature.settings.ui.settings_sections.AppearanceSettings
import dev.jotalac.feature.settings.ui.settings_sections.LanguageSettings
import dev.jotalac.feature.settings.ui.settings_sections.SyncSettings
import git_writer.shared.BuildKonfig
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.app_version_display
import git_writer.shared.generated.resources.arrow_right
import git_writer.shared.generated.resources.toggle_collapse
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsContent(
    userSettingsState: UserSettingsState,
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

            // show current app version
            Text(
                text = stringResource(Res.string.app_version_display) + BuildKonfig.APP_VERSION,
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = AppTheme.typography.body,
                color = AppTheme.colors.textMuted
            )
        }

        AppVerticalScrollbar(
            listState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
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
            modifier = Modifier.size(AppTheme.dimensions.iconLarge),
            tint = AppTheme.colors.textSecondary
        )

        Text(
            text = title,
            style = AppTheme.typography.title,
            color = AppTheme.colors.textSecondary,
        )

    }
}

@Composable
fun SettingsCollapsableSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.large,
        color = AppTheme.colors.surface,
        border = AppTheme.colors.border,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.large)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = AppTheme.typography.heading.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.text,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Icon(
                    painter = painterResource(Res.drawable.arrow_right),
                    contentDescription = stringResource(Res.string.toggle_collapse),
                    modifier = Modifier
                        .size(AppTheme.dimensions.iconSmall)
                        .graphicsLayer { rotationZ = rotation },
                    tint = AppTheme.colors.textSecondary
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
                    Divider(
                        color = AppTheme.colors.divider.copy(alpha = 0.5f)
                    )
                    content()
                }
            }
        }
    }
}