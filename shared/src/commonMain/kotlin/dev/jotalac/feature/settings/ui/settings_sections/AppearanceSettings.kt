package dev.jotalac.feature.settings.ui.settings_sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.domain.AppFontFamily
import dev.jotalac.core.domain.AppThemeAccentColor
import dev.jotalac.core.domain.AppThemeMode
import dev.jotalac.core.ui.theme.JetBrainsMonoFont
import dev.jotalac.feature.settings.platform.UseDynamicColorSetting
import dev.jotalac.feature.settings.ui.SectionTitle
import dev.jotalac.feature.settings.ui.SettingsCollapsableSection
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceSettings(
    selectedTheme: AppThemeMode,
    selectedThemeAccentColor: AppThemeAccentColor,
    selectedFontFamily: AppFontFamily,
    onThemeChange: (AppThemeMode) -> Unit,
    onThemeAccentColorChange: (AppThemeAccentColor) -> Unit,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    useDynamicColor: Boolean,
    onDynamicColorToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            stringResource(Res.string.settings_appearance_title),
            Res.drawable.palette
        )

        ThemeSettings(
            selectedTheme = selectedTheme,
            selectedColor = selectedThemeAccentColor,
            onThemeChange = onThemeChange,
            onColorChange = onThemeAccentColorChange,
            useDynamicColor = useDynamicColor,
            onUseDynamicColorToggle = onDynamicColorToggle
        )

        FontSettings(
            selectedFontFamily = selectedFontFamily,
            onFontFamilyChange = onFontFamilyChange
        )
    }
}

@Composable
private fun ThemeSettings(
    selectedTheme: AppThemeMode,
    selectedColor: AppThemeAccentColor,
    onThemeChange: (AppThemeMode) -> Unit,
    onColorChange: (AppThemeAccentColor) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorToggle: (Boolean) -> Unit
) {
    val themeLabels = mapOf(
        AppThemeMode.SYSTEM to stringResource(Res.string.settings_theme_system),
        AppThemeMode.LIGHT to stringResource(Res.string.settings_theme_light),
        AppThemeMode.DARK to stringResource(Res.string.settings_theme_dark)
    )

    SettingsCollapsableSection(
        title = stringResource(Res.string.settings_theme_title),
        subtitle = stringResource(Res.string.settings_theme_subtitle),
        initiallyExpanded = true
    ) {
        // theme mode selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.settings_theme_mode_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                AppThemeMode.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = selectedTheme == entry,
                        onClick = { onThemeChange(entry) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppThemeMode.entries.size
                        )
                    ) {
                        Text(
                            themeLabels[entry]!!,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        UseDynamicColorSetting(
            useDynamicColor,
            onUseDynamicColorToggle
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Custom Theme Color
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(Res.string.settings_accent_color_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppThemeAccentColor.entries.forEach { colorTheme ->
                    val isSelected = selectedColor == colorTheme

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorTheme.rgb))
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onColorChange(colorTheme) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontSettings(
    selectedFontFamily: AppFontFamily,
    onFontFamilyChange: (AppFontFamily) -> Unit
) {
    val fontLabels = mapOf(
        AppFontFamily.DEFAULT to stringResource(Res.string.settings_font_default),
        AppFontFamily.MONOSPACE to stringResource(Res.string.settings_font_monospace),
    )

    SettingsCollapsableSection(
        title = stringResource(Res.string.settings_font_title),
        subtitle = stringResource(Res.string.settings_font_subtitle),
        initiallyExpanded = false
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.settings_font_family_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                AppFontFamily.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = selectedFontFamily == entry,
                        onClick = { onFontFamilyChange(entry) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = fontLabels.size
                        )
                    ) {
                        Text(
                            text = fontLabels[entry]!!,
                            fontFamily = if (entry == AppFontFamily.MONOSPACE) JetBrainsMonoFont else FontFamily.Default,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}