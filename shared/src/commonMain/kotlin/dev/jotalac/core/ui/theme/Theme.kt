package dev.jotalac.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.jotalac.core.domain.AppFontFamily
import dev.jotalac.core.domain.AppThemeAccentColor


@Composable
expect fun determineColorScheme(
    isDark: Boolean,
    dynamicColor: Boolean,
    accentColor: AppThemeAccentColor
): ColorScheme

expect fun determineAppDimensions(): AppDimensions

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentColor: AppThemeAccentColor = AppThemeAccentColor.DEFAULT,
    fontFamily: AppFontFamily = AppFontFamily.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = determineColorScheme(
        isDark = darkTheme,
        dynamicColor = dynamicColor,
        accentColor = accentColor
    )
    val dimensions = determineAppDimensions()

    CompositionLocalProvider(LocalAppDimensions provides dimensions) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (fontFamily == AppFontFamily.DEFAULT) DefaultTypography else MonoSpaceTypography,
            content = content
        )
    }
}

