package dev.jotalac.feature.settings.ui.settings_sections

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.domain.AppLanguage
import dev.jotalac.core.domain.SpellcheckLanguage
import dev.jotalac.core.ui.design.*
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.settings.ui.SectionTitle
import dev.jotalac.feature.settings.ui.SettingsCollapsableSection
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
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
    val offColor = AppTheme.colors.textSecondary
    val sameAsAppColor = AppTheme.colors.accent

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
        MenuDivider()
        SelectBoxItem(sameAsAppTitle, sameAsAppColor) { select(SpellcheckLanguage.SameAsApp) }
        MenuDivider()

        AppLanguage.entries.filter { it.canSpellCheck }.forEach { language ->
            SelectBoxItem(language.title) { select(SpellcheckLanguage.Of(language)) }
        }
    }
}

/** A field showing [value] (in [valueColor], when specified) - clicked it opens [menuItems], which get a function that closes it. */
@Composable
private fun SettingsSelectBox(
    titleLabel: StringResource,
    value: String,
    valueColor: Color = Color.Unspecified,
    menuItems: @Composable (close: () -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val chevronRotation by animateFloatAsState(if (isExpanded) 180f else 0f)

    Text(
        text = stringResource(titleLabel),
        style = AppTheme.typography.label,
        color = AppTheme.colors.textSecondary
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { isExpanded = true },
            shape = AppTheme.shapes.medium,
            color = AppTheme.colors.field,
            border = AppTheme.colors.border,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidth = it.width },
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = AppTheme.dimensions.controlHeight)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(Res.drawable.arrow_down),
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp).rotate(chevronRotation),
                )
            }
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { fieldWidth.toDp() }),
        ) {
            menuItems { isExpanded = false }
        }
    }
}

@Composable
private fun SelectBoxItem(text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    MenuItem(
        text = { Text(text = text, color = color) },
        onClick = onClick,
    )
}
