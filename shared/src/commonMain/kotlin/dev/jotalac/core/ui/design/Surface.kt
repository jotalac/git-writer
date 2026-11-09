package dev.jotalac.core.ui.design

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.LocalContentColor

/**
 * A surface in [color] and [shape], outlined in [border] when it has one - its content is drawn in [contentColor].
 * Clicks do not go through it to what is below.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: Color = Color.Transparent,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .surface(shape, color, border, shadowElevation, AppTheme.colors.shadow)
                .semantics(mergeDescendants = false) { isTraversalGroup = true }
                .pointerInput(Unit) {},
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}

/** A surface that can be clicked - the hover and the press shown over it. */
@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: Color = Color.Transparent,
    shadowElevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .surface(shape, color, border, shadowElevation, AppTheme.colors.shadow)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}

/** The color of what is drawn on [background] - one of the theme's colors. */
@Composable
@ReadOnlyComposable
fun contentColorFor(background: Color): Color {
    val colors = AppTheme.colors
    return when (background) {
        colors.accent -> colors.onAccent
        colors.accentSoft -> colors.onAccentSoft
        colors.danger -> colors.onDanger
        colors.dangerSoft -> colors.onDangerSoft
        colors.background, colors.surface, colors.surfaceRaised, colors.surfaceSunken, colors.surfaceStrong, colors.field -> colors.text
        else -> LocalContentColor.current
    }
}

private fun Modifier.surface(shape: Shape, color: Color, border: Color, shadowElevation: Dp, shadowColor: Color): Modifier =
    then(
        if (shadowElevation > 0.dp) {
            Modifier.shadow(shadowElevation, shape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
        } else {
            Modifier
        }
    )
        .background(color, shape)
        .then(if (border != Color.Transparent) Modifier.border(1.dp, border, shape) else Modifier)
        .clip(shape)
