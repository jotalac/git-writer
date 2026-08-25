package dev.jotalac.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

val DefaultTypography = Typography()

val MonoSpaceTypography: Typography
    @Composable
    get() {
        val monoFont = JetBrainsMonoFont

        return Typography(
            displayLarge = DefaultTypography.displayLarge.copy(fontFamily = monoFont),
            displayMedium = DefaultTypography.displayMedium.copy(fontFamily = monoFont),
            displaySmall = DefaultTypography.displaySmall.copy(fontFamily = monoFont),
            headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = monoFont),
            headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = monoFont),
            headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = monoFont),
            titleLarge = DefaultTypography.titleLarge.copy(fontFamily = monoFont),
            titleMedium = DefaultTypography.titleMedium.copy(fontFamily = monoFont),
            titleSmall = DefaultTypography.titleSmall.copy(fontFamily = monoFont),
            bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = monoFont),
            bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = monoFont),
            bodySmall = DefaultTypography.bodySmall.copy(fontFamily = monoFont),
            labelLarge = DefaultTypography.labelLarge.copy(fontFamily = monoFont),
            labelMedium = DefaultTypography.labelMedium.copy(fontFamily = monoFont),
            labelSmall = DefaultTypography.labelSmall.copy(fontFamily = monoFont),
        )
    }