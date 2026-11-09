package dev.jotalac.core.ui.theme.colors

import androidx.compose.ui.graphics.Color
import dev.jotalac.core.domain.AppThemeAccentColor
import dev.jotalac.core.ui.theme.AppColors

/*
* Neutral grays - white pages in the light, charcoal in the dark - thin borders, and one accent color on top.
* */

/** The colors for the theme - light or [isDark], in the [accent] chosen in the settings. */
fun appColors(isDark: Boolean, accent: AppThemeAccentColor): AppColors {
    val accentSet = accentSet(accent, isDark)
    return if (isDark) darkNeutrals(accentSet) else lightNeutrals(accentSet)
}

private class AccentSet(val accent: Color, val onAccent: Color, val soft: Color, val onSoft: Color)

private fun lightNeutrals(accent: AccentSet) = AppColors(
    isDark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF6F6F6),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF2F2F2),
    surfaceStrong = Color(0xFFE6E6E6),
    field = Color(0xFFFFFFFF),
    text = Color(0xFF222222),
    textSecondary = Color(0xFF5C5C5C),
    textMuted = Color(0xFF9A9A9A),
    border = Color(0xFFE0E0E0),
    divider = Color(0xFFEBEBEB),
    accent = accent.accent,
    onAccent = accent.onAccent,
    accentSoft = accent.soft,
    onAccentSoft = accent.onSoft,
    danger = Color(0xFFD9343F),
    onDanger = Color(0xFFFFFFFF),
    dangerSoft = Color(0xFFFCE6E8),
    onDangerSoft = Color(0xFFAA1E2A),
    highlight = Color(0xFF9A6B00),
    highlightSoft = Color(0xFFFAF0D7),
    scrim = Color(0x52000000),
    shadow = Color(0x29000000),
)

private fun darkNeutrals(accent: AccentSet) = AppColors(
    isDark = true,
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF262626),
    surfaceRaised = Color(0xFF2A2A2A),
    // a step up from both the page and the raised dialogs it is in
    surfaceSunken = Color(0xFF323232),
    surfaceStrong = Color(0xFF3C3C3C),
    field = Color(0xFF1C1C1C),
    text = Color(0xFFDADADA),
    textSecondary = Color(0xFFB3B3B3),
    textMuted = Color(0xFF7C7C7C),
    border = Color(0xFF3A3A3A),
    divider = Color(0xFF303030),
    accent = accent.accent,
    onAccent = accent.onAccent,
    accentSoft = accent.soft,
    onAccentSoft = accent.onSoft,
    danger = Color(0xFFFB5A5F),
    onDanger = Color(0xFF2B0204),
    dangerSoft = Color(0xFF3D2325),
    onDangerSoft = Color(0xFFFFA9AC),
    highlight = Color(0xFFE0B556),
    highlightSoft = Color(0xFF3A3220),
    scrim = Color(0x99000000),
    shadow = Color(0x80000000),
)

private fun accentSet(accent: AppThemeAccentColor, isDark: Boolean): AccentSet = when (accent) {
    AppThemeAccentColor.DEFAULT -> if (isDark) {
        AccentSet(Color(0xFFE0907A), Color(0xFF2E0F07), Color(0xFF3F2C27), Color(0xFFF3C9BD))
    } else {
        AccentSet(Color(0xFFA6503B), Color(0xFFFFFFFF), Color(0xFFF6E7E2), Color(0xFF7A3423))
    }

    AppThemeAccentColor.PURPLE -> if (isDark) {
        AccentSet(Color(0xFFA8A2FF), Color(0xFF171340), Color(0xFF2F2C4A), Color(0xFFD9D6FF))
    } else {
        AccentSet(Color(0xFF5048CC), Color(0xFFFFFFFF), Color(0xFFECEBFB), Color(0xFF332C99))
    }

    AppThemeAccentColor.GREEN -> if (isDark) {
        AccentSet(Color(0xFFAFC764), Color(0xFF1A2000), Color(0xFF2F3420), Color(0xFFD9E6A6))
    } else {
        AccentSet(Color(0xFF5B7014), Color(0xFFFFFFFF), Color(0xFFEEF2DD), Color(0xFF3B4A08))
    }

    AppThemeAccentColor.PINK -> if (isDark) {
        AccentSet(Color(0xFFF09AB6), Color(0xFF3B0919), Color(0xFF45272F), Color(0xFFFFD4E1))
    } else {
        AccentSet(Color(0xFFB8335F), Color(0xFFFFFFFF), Color(0xFFFAE6EC), Color(0xFF861C42))
    }
}
