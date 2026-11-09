package dev.jotalac.core.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import dev.jotalac.core.domain.AppFontFamily
import dev.jotalac.core.domain.AppThemeAccentColor
import dev.jotalac.core.ui.theme.colors.appColors

expect fun determineAppDimensions(): AppDimensions

/**
 * The app's "warm paper" look - its colors, text styles, shapes and sizes, read through [AppTheme.colors] and the
 * others.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AppThemeAccentColor = AppThemeAccentColor.DEFAULT,
    fontFamily: AppFontFamily = AppFontFamily.DEFAULT,
    content: @Composable () -> Unit
) {
    val colors = remember(darkTheme, accentColor) { appColors(darkTheme, accentColor) }
    val typography = appTypography(fontFamily)
    val selectionColors = remember(colors) {
        TextSelectionColors(handleColor = colors.accent, backgroundColor = colors.accent.copy(alpha = 0.3f))
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppShapes provides AppShapes(),
        LocalAppDimensions provides determineAppDimensions(),
        LocalContentColor provides colors.text,
        LocalTextStyle provides typography.body,
        LocalIndication provides AppIndication,
        LocalTextSelectionColors provides selectionColors,
        content = content,
    )
}

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current

    val typography: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current

    val shapes: AppShapes
        @Composable @ReadOnlyComposable get() = LocalAppShapes.current

    val dimensions: AppDimensions
        @Composable @ReadOnlyComposable get() = LocalAppDimensions.current
}
