package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.markdownAlertColors
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.feature.editor.ui.utils.getHeaderFontSize

/*
* The rendered markdown (tables, images, maths) in the app's colors and text styles.
* */

@Composable
internal fun appMarkdownColors(): MarkdownColors {
    val colors = AppTheme.colors
    return DefaultMarkdownColors(
        text = colors.text,
        codeBackground = colors.surfaceSunken,
        inlineCodeBackground = colors.surfaceSunken,
        dividerColor = colors.divider,
        tableBackground = colors.surface,
        alert = markdownAlertColors(darkTheme = colors.isDark),
        highlightText = colors.onAccentSoft,
        highlightBackground = colors.accentSoft,
    )
}

@Composable
internal fun appMarkdownTypography(): MarkdownTypography {
    val typography = AppTheme.typography
    val text = typography.bodyLarge.copy(color = AppTheme.colors.text)
    return DefaultMarkdownTypography(
        h1 = getHeaderFontSize(1),
        h2 = getHeaderFontSize(2),
        h3 = getHeaderFontSize(3),
        h4 = getHeaderFontSize(4),
        h5 = getHeaderFontSize(5),
        h6 = getHeaderFontSize(6),
        text = text,
        code = typography.body.copy(fontFamily = FontFamily.Monospace),
        inlineCode = text.copy(fontFamily = FontFamily.Monospace, fontSize = TextUnit.Unspecified),
        quote = typography.body.copy(fontStyle = FontStyle.Italic),
        paragraph = text,
        ordered = text,
        bullet = text,
        list = text,
        textLink = TextLinkStyles(
            style = SpanStyle(color = AppTheme.colors.accent, textDecoration = TextDecoration.Underline),
        ),
        table = text,
        alertTitle = text.copy(fontWeight = FontWeight.Bold),
    )
}
