package dev.jotalac.feature.settings.ui.settings_sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jotalac.core.domain.AppLanguage
import dev.jotalac.core.domain.SpellcheckLanguage
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.settings.ui.SectionTitle
import dev.jotalac.feature.settings.ui.SettingsCollapsableSection
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun LanguageSettings(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    selectedSpellcheckLanguage: SpellcheckLanguage,
    onSpellcheckLanguageChange: (SpellcheckLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            stringResource(Res.string.settings_language_title),
            Res.drawable.globe

        )

        SettingsCollapsableSection(
            title = stringResource(Res.string.settings_language_section_title),
            subtitle = if (isDesktopPlatform) stringResource(Res.string.settings_language_subtitle_desktop)
                else stringResource(Res.string.settings_language_subtitle),
            initiallyExpanded = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSelectBox(
                    titleLabel = Res.string.settings_language_label,
                    value = selectedLanguage.title,
                ) { close ->
                    AppLanguage.entries.forEach { language ->
                        SelectBoxItem(language.title) {
                            onLanguageChange(language)
                            close()
                        }
                    }
                }

                // on mobile the keyboard checks the spelling
                if (isDesktopPlatform) {
                    Spacer(Modifier.height(8.dp))

                    SpellcheckLanguageSelectBox(
                        selected = selectedSpellcheckLanguage,
                        onChange = onSpellcheckLanguageChange,
                    )
                }
            }
        }
    }
}

// no spell check and the app's language first, each in its own color, then the languages that can be checked
@Composable
private fun SpellcheckLanguageSelectBox(
    selected: SpellcheckLanguage,
    onChange: (SpellcheckLanguage) -> Unit,
) {
    val offTitle = stringResource(Res.string.no_spell_check)
    val sameAsAppTitle = stringResource(Res.string.settings_spellcheck_language_same_as_app)
    val offColor = MaterialTheme.colorScheme.secondary
    val sameAsAppColor = MaterialTheme.colorScheme.primary

    SettingsSelectBox(
        titleLabel = Res.string.settings_spellcheck_language_label,
        value = when (selected) {
            SpellcheckLanguage.Off -> offTitle
            SpellcheckLanguage.SameAsApp -> sameAsAppTitle
            is SpellcheckLanguage.Of -> selected.language.title
        },
        valueColor = when (selected) {
            SpellcheckLanguage.Off -> offColor
            SpellcheckLanguage.SameAsApp -> sameAsAppColor
            is SpellcheckLanguage.Of -> Color.Unspecified
        },
    ) { close ->
        fun select(language: SpellcheckLanguage) {
            onChange(language)
            close()
        }

        SelectBoxItem(offTitle, offColor) { select(SpellcheckLanguage.Off) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SelectBoxItem(sameAsAppTitle, sameAsAppColor) { select(SpellcheckLanguage.SameAsApp) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        AppLanguage.entries.filter { it.canSpellCheck }.forEach { language ->
            SelectBoxItem(language.title) { select(SpellcheckLanguage.Of(language)) }
        }
    }
}

/** A dropdown showing [value] (in [valueColor], when specified) - [menuItems] get a function that closes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSelectBox(
    titleLabel: StringResource,
    value: String,
    valueColor: Color = Color.Unspecified,
    menuItems: @Composable (close: () -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Text(
        text = stringResource(titleLabel),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            colors = if (valueColor != Color.Unspecified) {
                ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedTextColor = valueColor,
                    unfocusedTextColor = valueColor
                )
            } else {
                ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            menuItems { isExpanded = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectBoxItem(text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = text, color = color) },
        onClick = onClick,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}
