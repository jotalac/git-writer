package dev.jotalac.core.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.jotalac.core.domain.AppThemeAccentColor
import dev.jotalac.core.ui.theme.colors.*

@Composable
actual fun determineColorScheme(
    isDark: Boolean,
    dynamicColor: Boolean,
    accentColor: AppThemeAccentColor
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        accentColor == AppThemeAccentColor.DEFAULT -> if (isDark) defaultDarkScheme else defaultLightScheme
        accentColor == AppThemeAccentColor.PURPLE -> if (isDark) purpleDarkScheme else purpleLightScheme
        accentColor == AppThemeAccentColor.GREEN -> if (isDark) greenDarkScheme else greenLightScheme
        accentColor == AppThemeAccentColor.PINK -> if (isDark) pinkDarkScheme else pinkLightScheme
        else -> defaultDarkScheme
    }
}

actual fun determineAppDimensions(): AppDimensions = MobileDimensions