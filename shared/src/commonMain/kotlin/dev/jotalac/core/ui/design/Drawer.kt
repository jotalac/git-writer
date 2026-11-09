package dev.jotalac.core.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.jotalac.core.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// a fling faster than this settles the drawer in its direction
private const val SETTLE_VELOCITY = 800f

/** Whether a [Drawer] is open - how far, while it moves. */
@Stable
class DrawerState {
    /** 0 closed, 1 open. */
    internal val progress = Animatable(0f)

    val isOpen: Boolean get() = progress.targetValue == 1f

    suspend fun open() = progress.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))

    suspend fun close() = progress.animateTo(0f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
}

@Composable
fun rememberDrawerState(): DrawerState = remember { DrawerState() }

/**
 * A panel sliding in from the left over the [content], which a scrim darkens - a swipe opens and closes it, a tap
 * on the scrim or the back gesture closes it.
 */
@Composable
fun Drawer(
    state: DrawerState,
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerWidth: Dp = AppTheme.dimensions.navDrawerWidth,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val widthPx = with(LocalDensity.current) { drawerWidth.toPx() }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.isOpen,
        onBackCompleted = { scope.launch { state.close() } },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch { state.progress.snapTo((state.progress.value + delta / widthPx).coerceIn(0f, 1f)) }
                },
                onDragStopped = { velocity ->
                    val opens = when {
                        velocity > SETTLE_VELOCITY -> true
                        velocity < -SETTLE_VELOCITY -> false
                        else -> state.progress.value > 0.5f
                    }
                    if (opens) state.open() else state.close()
                },
            ),
    ) {
        content()

        val progress = state.progress.value
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.scrim.copy(alpha = colors.scrim.alpha * progress))
                    .pointerInput(state) { detectTapGestures { scope.launch { state.close() } } }
            )
            Surface(
                color = colors.surface,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .offset { IntOffset((-(1f - progress) * widthPx).roundToInt(), 0) },
            ) {
                // a flat panel, a line at its edge
                Row {
                    Box(Modifier.weight(1f)) { drawerContent() }
                    VerticalDivider(color = colors.border)
                }
            }
        }
    }
}
