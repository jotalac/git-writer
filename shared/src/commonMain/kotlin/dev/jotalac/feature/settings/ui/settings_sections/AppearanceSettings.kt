package dev.jotalac.feature.settings.ui.settings_sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.settings.ui.SectionTitle
import dev.jotalac.feature.settings.ui.SettingsCollapsableSection
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceSettings() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            stringResource(Res.string.settings_appearance_title),
            Res.drawable.palette
        )

        ThemeSettings()

        FontSettings()
    }
}

private val THEME_COLORS = listOf(
    Color(0xFF6750A4), // Purple
    Color(0xFF00639B), // Blue
    Color(0xFF006A60), // Teal
    Color(0xFF3B6939), // Green
    Color(0xFF8B5000), // Amber
    Color(0xFF984061), // Rose
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSettings() {
    var selectedThemeIndex by remember { mutableStateOf(0) }
    val themeOptions = listOf(
        stringResource(Res.string.settings_theme_system),
        stringResource(Res.string.settings_theme_light),
        stringResource(Res.string.settings_theme_dark)
    )
    var selectedColor by remember { mutableStateOf(THEME_COLORS.first()) }

    SettingsCollapsableSection(
        title = stringResource(Res.string.settings_theme_title),
        subtitle = stringResource(Res.string.settings_theme_subtitle),
        initiallyExpanded = true
    ) {
        // Theme Mode Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.settings_theme_mode_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                themeOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedThemeIndex == index,
                        onClick = { selectedThemeIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = themeOptions.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }
        }

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
                THEME_COLORS.forEach { color ->
                    val isSelected = selectedColor == color

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
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
                            .clickable { selectedColor = color },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSettings() {
    var selectedFontIndex by remember { mutableStateOf(0) }
    val fontOptions = listOf(
        stringResource(Res.string.settings_font_default),
        stringResource(Res.string.settings_font_monospace),
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
                fontOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedFontIndex == index,
                        onClick = { selectedFontIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = fontOptions.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}