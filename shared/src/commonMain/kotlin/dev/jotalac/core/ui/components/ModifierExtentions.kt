package dev.jotalac.core.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import dev.jotalac.core.utils.isDesktopPlatform


fun Modifier.onContextMenuGesture(onOpen: (DpOffset) -> Unit): Modifier =
    if (isDesktopPlatform) onContextMenuOpen(onOpen) else onLongPress(onOpen)

/** A right click, at its position from the element's top-left. */
fun Modifier.onContextMenuOpen(onEvent: (DpOffset) -> Unit): Modifier = composed {
    val density = LocalDensity.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    val pressChange = event.changes.first()

                    if (!pressChange.isConsumed) {
                        currentOnEvent(pressChange.position.toDpOffset(density))
                        pressChange.consume()
                    }
                }
            }
        }
    }
}


fun Modifier.onLongPress(onLongPress: (DpOffset) -> Unit): Modifier = composed {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture

            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            currentOnLongPress(longPress.position.toDpOffset(density))
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

private fun Offset.toDpOffset(density: Density): DpOffset = with(density) { DpOffset(x.toDp(), y.toDp()) }
