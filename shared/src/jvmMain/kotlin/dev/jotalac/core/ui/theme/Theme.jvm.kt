package dev.jotalac.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun determineColorScheme(
    isDark: Boolean,
    dynamicColor: Boolean
): ColorScheme {
    return if (isDark) darkScheme else lightScheme
}

actual fun determineAppDimensions(): AppDimensions = DesktopDimensions