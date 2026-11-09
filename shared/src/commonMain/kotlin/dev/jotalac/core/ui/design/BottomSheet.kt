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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jotalac.core.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// a drag down further than this part of the sheet, or a fling, closes it
private const val DISMISS_FRACTION = 0.3f
private const val DISMISS_VELOCITY = 1500f

/**
 * A sheet sliding up from the bottom over a scrim. A tap on the scrim, the back key or dragging it down close it -
 * it slides away first, then [onDismissRequest] removes it.
 */
@Composable
fun BottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var sheetHeight by remember { mutableIntStateOf(0) }
    // how far the sheet is moved down from where it is shown
    val offset = remember { Animatable(0f) }

    fun hide() {
        scope.launch {
            offset.animateTo(sheetHeight.toFloat(), tween(durationMillis = 180))
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = ::hide,
        properties = appDialogProperties(dismissible = true, fullScreen = true, scrim = colors.scrim),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { hide() } },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = AppTheme.shapes.sheet,
                color = colors.surfaceRaised,
                border = colors.border,
                shadowElevation = 12.dp,
                modifier = modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        if (sheetHeight == 0) {
                            sheetHeight = size.height
                            scope.launch {
                                offset.snapTo(size.height.toFloat())
                                offset.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
                            }
                        }
                    }
                    // hidden until it is measured and can slide up from below
                    .graphicsLayer { alpha = if (sheetHeight == 0) 0f else 1f }
                    .offset { IntOffset(0, offset.value.roundToInt().coerceAtLeast(0)) }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            scope.launch { offset.snapTo((offset.value + delta).coerceAtLeast(0f)) }
                        },
                        onDragStopped = { velocity ->
                            if (offset.value > sheetHeight * DISMISS_FRACTION || velocity > DISMISS_VELOCITY) {
                                hide()
                            } else {
                                offset.animateTo(0f)
                            }
                        },
                    )
                    // a tap on the sheet is not a tap on the scrim
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(bottom = 8.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 10.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .background(colors.textMuted.copy(alpha = 0.5f), AppTheme.shapes.pill)
                    )
                    content()
                }
            }
        }
    }
}
