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
import androidx.compose.ui.unit.dp
import dev.jotalac.core.domain.AppLanguage
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
    selectedSpellcheckLanguage: AppLanguage?,
    onSpellcheckLanguageChange: (AppLanguage?) -> Unit,
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
                LanguageSettingsSelectBox(
                    titleLabel = Res.string.settings_language_label,
                    selectedLanguage = selectedLanguage,
                    onLanguageChange = { language -> language?.let(onLanguageChange) },
                )

                if (isDesktopPlatform) {
                    Spacer(Modifier.height(8.dp))

                    LanguageSettingsSelectBox(
                        titleLabel = Res.string.settings_spellcheck_language_label,
                        selectedLanguage = selectedSpellcheckLanguage,
                        onLanguageChange = onSpellcheckLanguageChange,
                        needSpellCheck = true,
                        allowSameAsApp = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSettingsSelectBox(
    titleLabel: StringResource,
    selectedLanguage: AppLanguage?,
    onLanguageChange: (AppLanguage?) -> Unit,
    needSpellCheck: Boolean = false,
    allowSameAsApp: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }
    val followsAppLanguage = allowSameAsApp && selectedLanguage == null

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
            value = selectedLanguage?.title
                ?: stringResource(Res.string.settings_spellcheck_language_same_as_app),
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            colors = if (followsAppLanguage) {
                ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedTextColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = MaterialTheme.colorScheme.primary
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
            if (allowSameAsApp) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.settings_spellcheck_language_same_as_app),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        onLanguageChange(null)
                        isExpanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            AppLanguage.entries.filter { it.canSpellCheck || !needSpellCheck }.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.title) },
                    onClick = {
                        onLanguageChange(language)
                        isExpanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}