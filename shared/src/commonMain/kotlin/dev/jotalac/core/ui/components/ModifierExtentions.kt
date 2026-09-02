package dev.jotalac.core.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset

fun Modifier.onContextMenuOpen(onEvent: (DpOffset) -> Unit): Modifier = composed {
    val density = LocalDensity.current
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    val pressChange = event.changes.first()

                    if (!pressChange.isConsumed) {
                        val position = pressChange.position
                        val offset = with(density) {
                            DpOffset(position.x.toDp(), (position.y - 10).toDp())
                        }
                        onEvent(offset)
                        pressChange.consume()
                    }
                }
            }
        }
    }
}