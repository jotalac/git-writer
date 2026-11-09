package dev.jotalac.feature.settings.ui.settings_sections

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.jotalac.core.domain.AppFontFamily
import dev.jotalac.core.domain.AppThemeAccentColor
import dev.jotalac.core.domain.AppThemeMode
import dev.jotalac.core.ui.design.*
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.JetBrainsMonoFont
import dev.jotalac.core.ui.theme.colors.appColors
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
    editorFullWidth: Boolean,
    onEditorFullWidthChange: (Boolean) -> Unit,
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
        )

        FontSettings(
            selectedFontFamily = selectedFontFamily,
            onFontFamilyChange = onFontFamilyChange
        )

        EditorWidthSettings(
            editorFullWidth = editorFullWidth,
            onEditorFullWidthChange = onEditorFullWidthChange
        )
    }
}

@Composable
private fun ThemeSettings(
    selectedTheme: AppThemeMode,
    selectedColor: AppThemeAccentColor,
    onThemeChange: (AppThemeMode) -> Unit,
    onColorChange: (AppThemeAccentColor) -> Unit,
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
                style = AppTheme.typography.label,
                color = AppTheme.colors.textSecondary
            )

            SegmentedControl(
                options = AppThemeMode.entries,
                selected = selectedTheme,
                onSelect = onThemeChange,
                label = { themeLabels.getValue(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // the accent color
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(Res.string.settings_accent_color_label),
                style = AppTheme.typography.label,
                color = AppTheme.colors.textSecondary
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
                            .background(appColors(AppTheme.colors.isDark, colorTheme).accent)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 3.dp,
                                        color = AppTheme.colors.text,
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
        initiallyExpanded = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.settings_font_family_label),
                style = AppTheme.typography.label,
                color = AppTheme.colors.textSecondary
            )

            SegmentedControl(
                options = AppFontFamily.entries,
                selected = selectedFontFamily,
                onSelect = onFontFamilyChange,
                label = { fontLabels.getValue(it) },
                // each font shown in itself
                labelFontFamily = { if (it == AppFontFamily.MONOSPACE) JetBrainsMonoFont else FontFamily.Default },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EditorWidthSettings(
    editorFullWidth: Boolean,
    onEditorFullWidthChange: (Boolean) -> Unit
) {
    SettingsCollapsableSection(
        title = stringResource(Res.string.settings_editor_width_title),
        subtitle = stringResource(Res.string.settings_editor_width_subtitle),
        initiallyExpanded = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.settings_editor_full_width_label),
                style = AppTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = editorFullWidth,
                onCheckedChange = onEditorFullWidthChange
            )
        }
    }
}
