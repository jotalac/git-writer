package dev.jotalac.feature.editor_sidebar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.dimensions

@Composable
fun EditorSidebar(
    isVisible: Boolean,
) {
    val initialWidth = MaterialTheme.dimensions.navDrawerWidth
    var sidebarWidth by remember(initialWidth) { mutableStateOf(initialWidth) }
    // for converting drag pixels to dp
    val density = LocalDensity.current

    // make it max width of the sidebar to 75% of the screen width
    val windowSize = LocalWindowInfo.current.containerSize
    val maxSidebarWidth = with(density) { (windowSize.width * 0.75f).toInt().toDp() }


    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally() + expandHorizontally(),
        exit = slideOutHorizontally() + shrinkHorizontally()
    ) {
        Box(
            modifier = Modifier
                .width(sidebarWidth.coerceAtMost(maxSidebarWidth))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {

            Row {
                SidebarContent(modifier = Modifier.weight(1f))

                SidebarDraggableHandle(
                    onDragDelta = { delta ->
                        sidebarWidth = (sidebarWidth + delta).coerceIn(200.dp, maxSidebarWidth)
                    },
                    density = density,
                )
            }

        }
    }
}

@Composable
private fun SidebarDraggableHandle(
    onDragDelta: (Dp) -> Unit,
    density: Density,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    val lineColor by animateColorAsState(
        targetValue = if (isDragging) {
            MaterialTheme.colorScheme.primary
        } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
        label = "dragHandleColor"
    )

    Box(
        modifier = modifier
            .width(8.dp)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon.Crosshair)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val dragAmountDp = with(density) { dragAmount.toDp() }
                        onDragDelta(dragAmountDp)
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(lineColor)
        )
    }
}