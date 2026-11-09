package dev.jotalac.core.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.LocalContentColor
import dev.jotalac.core.ui.theme.LocalTextStyle

/** How much a button stands out - one [Primary] per place, the others for the rest. */
enum class ButtonStyle { Primary, Secondary, Ghost, Danger }

private const val DISABLED_ALPHA = 0.45f

/**
 * A button with slightly rounded corners. While [isLoading] it shows a spinner in front of its content and cannot be
 * clicked.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val (container, contentColor) = when (style) {
        ButtonStyle.Primary -> colors.accent to colors.onAccent
        // outlined on the light theme, a lighter fill on the dark one
        ButtonStyle.Secondary -> (if (colors.isDark) colors.surfaceStrong else colors.surfaceRaised) to colors.text
        ButtonStyle.Ghost -> Color.Transparent to colors.text
        ButtonStyle.Danger -> colors.danger to colors.onDanger
    }
    val isClickable = enabled && !isLoading

    Surface(
        onClick = onClick,
        enabled = isClickable,
        shape = AppTheme.shapes.medium,
        color = container,
        contentColor = contentColor,
        border = if (style == ButtonStyle.Secondary && !colors.isDark) colors.border else Color.Transparent,
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .defaultMinSize(minHeight = AppTheme.dimensions.controlHeight),
    ) {
        CompositionLocalProvider(LocalTextStyle provides AppTheme.typography.label) {
            Row(
                modifier = Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLoading) Spinner(color = contentColor, size = 16.dp)
                content()
            }
        }
    }
}

/** A button of just its text - [ButtonStyle.Ghost]. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        style = ButtonStyle.Ghost,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        content = content,
    )
}

/** A button of an icon - no background until it is hovered or pressed. */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = AppTheme.dimensions.controlHeight,
    shape: Shape = AppTheme.shapes.small,
    contentColor: Color = LocalContentColor.current,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** A round button floating over the content, in [containerColor]. */
@Composable
fun FloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colors.accent,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = AppTheme.shapes.large,
    size: Dp = 52.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
