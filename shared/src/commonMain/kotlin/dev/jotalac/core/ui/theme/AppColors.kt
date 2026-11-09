package dev.jotalac.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colors of the app, by what they are for - see [dev.jotalac.core.ui.theme.colors.appColors] for the palette.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,

    // --- surfaces, from the lowest to the highest ---
    /** Behind everything - the editor's page. */
    val background: Color,
    /** Panels next to the page - the sidebar, the tabs, the bars. */
    val surface: Color,
    /** What floats above - dialogs, sheets, menus, snackbars. */
    val surfaceRaised: Color,
    /** A fill set into a surface - code, the track of a switch or a segmented control. */
    val surfaceSunken: Color,
    /** A strong fill on a surface - a selected row, a secondary button in the dark. */
    val surfaceStrong: Color,
    /** Behind the text of a field. */
    val field: Color,

    // --- content ---
    val text: Color,
    /** Labels, descriptions, icons next to text. */
    val textSecondary: Color,
    /** Hints, placeholders, hidden markdown syntax. */
    val textMuted: Color,
    /** The 1px outline of fields, dialogs, menus and cards. */
    val border: Color,
    /** A line between parts of a surface - lighter than a [border]. */
    val divider: Color,

    // --- the chosen accent ---
    val accent: Color,
    val onAccent: Color,
    /** A tonal fill in the accent's hue - a selected item, a tonal button. */
    val accentSoft: Color,
    val onAccentSoft: Color,

    // --- states ---
    val danger: Color,
    val onDanger: Color,
    val dangerSoft: Color,
    val onDangerSoft: Color,
    /** A warm ochre for what needs attention without being an error - maths, an unreachable remote. */
    val highlight: Color,
    val highlightSoft: Color,

    /** Over the app behind a dialog or a sheet. */
    val scrim: Color,
    val shadow: Color,
)

internal val LocalAppColors = staticCompositionLocalOf<AppColors> { error("No AppColors - wrap the content in AppTheme") }
