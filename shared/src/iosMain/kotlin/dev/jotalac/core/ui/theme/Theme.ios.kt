package dev.jotalac.core.ui.theme

@Composable
actual fun determineColorScheme(
    isDark: Boolean,
    dynamicColor: Boolean,
    accentColor: ThemeAccentColor
): ColorScheme {
    return when (accentColor) {
        ThemeAccentColor.DEFAULT -> if (isDark) defaultDarkScheme else defaultLightScheme
        ThemeAccentColor.PURPLE -> if (isDark) purpleDarkScheme else purpleLightScheme
    }
}

actual fun determineAppDimensions(): AppDimensions = MobileDimensions