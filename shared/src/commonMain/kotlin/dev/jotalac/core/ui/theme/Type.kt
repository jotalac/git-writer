package dev.jotalac.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.jotalac.core.domain.AppFontFamily
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.jetbrains_mono_bold
import git_writer.shared.generated.resources.jetbrains_mono_light
import git_writer.shared.generated.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

val JetBrainsMonoFont: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(Res.font.jetbrains_mono_bold, FontWeight.Bold),
        Font(Res.font.jetbrains_mono_light, FontWeight.Light),
    )

@Immutable
data class AppTypography(
    /** A big empty-state title. */
    val display: TextStyle,
    /** A dialog's or a screen's title. */
    val title: TextStyle,
    /** A section's heading. */
    val heading: TextStyle,
    /** The note's text. */
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    /** Buttons, tabs, field labels. */
    val label: TextStyle,
    val labelSmall: TextStyle,
)

/** The text styles, in the font chosen in the settings. */
@Composable
fun appTypography(font: AppFontFamily): AppTypography {
    val family = if (font == AppFontFamily.DEFAULT) FontFamily.Default else JetBrainsMonoFont
    fun style(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal, letterSpacing: Float = 0f) =
        TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = lineHeight.sp, fontWeight = weight, letterSpacing = letterSpacing.sp)

    return AppTypography(
        display = style(26, 32, FontWeight.SemiBold, -0.3f),
        title = style(17, 24, FontWeight.SemiBold),
        heading = style(14, 20, FontWeight.SemiBold),
        bodyLarge = style(16, 24),
        body = style(14, 20),
        bodySmall = style(12, 16),
        label = style(13, 18, FontWeight.Medium),
        labelSmall = style(12, 16, FontWeight.Medium),
    )
}

internal val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("No AppTypography - wrap the content in AppTheme")
}
