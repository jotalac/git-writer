package dev.jotalac.core.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.LocalContentColor
import dev.jotalac.core.ui.theme.LocalTextStyle
import kotlin.math.max

/**
 * A menu floating below its anchor - the parent it is placed in - moved by [offset] from the anchor's bottom left
 * corner, and above the anchor when there is no room below.
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    if (!visibility.currentState && !visibility.targetState) return

    val density = LocalDensity.current
    val positionProvider = remember(offset, density) { MenuPositionProvider(offset, density) }
    Popup(popupPositionProvider = positionProvider, onDismissRequest = onDismissRequest, properties = properties) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(tween(90)) + scaleIn(tween(120), initialScale = 0.96f, transformOrigin = TransformOrigin(0f, 0f)),
            exit = fadeOut(tween(75)),
        ) {
            Surface(
                shape = AppTheme.shapes.large,
                color = AppTheme.colors.surfaceRaised,
                border = AppTheme.colors.border,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = modifier
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())
                        .padding(4.dp),
                    content = content,
                )
            }
        }
    }
}

/** An item of a [DropdownMenu] - [text] with an optional icon on either side. */
@Composable
fun MenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    contentColor: Color = AppTheme.colors.text,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp),
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides AppTheme.typography.body.copy(fontSize = AppTheme.dimensions.listItemTextSize),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = AppTheme.dimensions.contextMenuItemHeight)
                .clip(AppTheme.shapes.small)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = null,
                    indication = LocalIndication.current,
                    onClick = onClick,
                )
                .alpha(if (enabled) 1f else 0.45f)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            Box(Modifier.weight(1f)) { text() }
            trailingIcon?.invoke()
        }
    }
}

/** A line between groups of a menu's items. */
@Composable
fun MenuDivider(modifier: Modifier = Modifier) {
    Divider(modifier.padding(horizontal = 6.dp, vertical = 4.dp))
}

// x from the anchor's left, y from its bottom - or above its top when the menu does not fit below
private class MenuPositionProvider(private val offset: DpOffset, private val density: Density) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val dx = with(density) { offset.x.roundToPx() }
        val dy = with(density) { offset.y.roundToPx() }
        val x = (anchorBounds.left + dx).coerceIn(0, max(0, windowSize.width - popupContentSize.width))
        val below = anchorBounds.bottom + dy
        val above = anchorBounds.top - dy - popupContentSize.height
        val y = when {
            below + popupContentSize.height <= windowSize.height -> below
            above >= 0 -> above
            else -> max(0, windowSize.height - popupContentSize.height)
        }
        return IntOffset(x, y)
    }
}
