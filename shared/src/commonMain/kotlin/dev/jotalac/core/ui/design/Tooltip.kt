package dev.jotalac.core.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.isDesktopPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Where a [Tooltip] is shown next to its content. */
enum class TooltipPlacement { Above, Below, Left, Right }

// how long a tooltip shown by a long press stays
private val TOUCH_TOOLTIP_TIME = 1500.milliseconds

/**
 * [text] in a small dark label next to the [content] - after hovering it for [delayTime] on desktop, on a long press
 * on mobile. The long press is watched before the content, which still gets its own clicks.
 */
@Composable
fun Tooltip(
    text: String,
    modifier: Modifier = Modifier,
    delayTime: Duration = 1.seconds,
    placement: TooltipPlacement = TooltipPlacement.Above,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isShownByTouch by remember { mutableStateOf(false) }
    var isShownByHover by remember { mutableStateOf(false) }
    val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis

    // leaving earlier cancels the wait
    LaunchedEffect(isHovered, delayTime) {
        isShownByHover = false
        if (isHovered && isDesktopPlatform) {
            delay(delayTime)
            isShownByHover = true
        }
    }
    LaunchedEffect(isShownByTouch) {
        if (isShownByTouch) {
            delay(TOUCH_TOOLTIP_TIME)
            isShownByTouch = false
        }
    }

    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .then(
                if (isDesktopPlatform) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val released = withTimeoutOrNull(longPressTimeout) {
                                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            }
                            if (released == null) isShownByTouch = true
                        }
                    }
                }
            ),
    ) {
        content()
        TooltipPopup(text, isVisible = isShownByHover || isShownByTouch, placement)
    }
}

@Composable
private fun TooltipPopup(text: String, isVisible: Boolean, placement: TooltipPlacement) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = isVisible
    if (!visibility.currentState && !visibility.targetState) return

    val density = LocalDensity.current
    val positionProvider = remember(placement, density) { TooltipPositionProvider(placement, density) }
    Popup(popupPositionProvider = positionProvider) {
        AnimatedVisibility(visibleState = visibility, enter = fadeIn(tween(120)), exit = fadeOut(tween(90))) {
            Surface(
                shape = AppTheme.shapes.small,
                color = AppTheme.colors.text,
                contentColor = AppTheme.colors.background,
                shadowElevation = 4.dp,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(
                    text = text,
                    style = AppTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// next to the anchor on its side, centered on it, kept in the window
private class TooltipPositionProvider(
    private val placement: TooltipPlacement,
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = with(density) { 8.dp.roundToPx() }
        val centerX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val centerY = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
        val above = anchorBounds.top - gap - popupContentSize.height
        val below = anchorBounds.bottom + gap
        val (x, y) = when (placement) {
            TooltipPlacement.Above -> centerX to if (above >= 0) above else below
            TooltipPlacement.Below -> centerX to if (below + popupContentSize.height <= windowSize.height) below else above
            TooltipPlacement.Left -> (anchorBounds.left - gap - popupContentSize.width) to centerY
            TooltipPlacement.Right -> (anchorBounds.right + gap) to centerY
        }
        return IntOffset(
            x.coerceIn(0, max(0, windowSize.width - popupContentSize.width)),
            y.coerceIn(0, max(0, windowSize.height - popupContentSize.height)),
        )
    }
}
