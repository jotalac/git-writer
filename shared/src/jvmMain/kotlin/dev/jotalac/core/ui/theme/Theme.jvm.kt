package dev.jotalac.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import dev.jotalac.core.domain.AppThemeAccentColor
import dev.jotalac.core.ui.theme.colors.*

@Composable
actual fun determineColorScheme(
    isDark: Boolean,
    dynamicColor: Boolean,
    accentColor: AppThemeAccentColor
): ColorScheme {
    return when (accentColor) {
        AppThemeAccentColor.DEFAULT -> if (isDark) defaultDarkScheme else defaultLightScheme
        AppThemeAccentColor.PURPLE -> if (isDark) purpleDarkScheme else purpleLightScheme
        AppThemeAccentColor.GREEN -> if (isDark) greenDarkScheme else greenLightScheme
        AppThemeAccentColor.PINK -> if (isDark) pinkDarkScheme else pinkLightScheme

    }
}

actual fun determineAppDimensions(): AppDimensions = DesktopDimensions