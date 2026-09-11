package dev.jotalac.core.ui.window

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window

val LocalDesktopAwtWindow = staticCompositionLocalOf<Window?> { null }

// implement the window dragging
@Composable
internal actual fun Modifier.draggableWindow(): Modifier {
    val window = LocalDesktopAwtWindow.current ?: return this

    return pointerInput(window) {
        var lastPointer = MouseInfo.getPointerInfo().location
        detectDragGestures(
            onDragStart = { lastPointer = MouseInfo.getPointerInfo().location },
            onDrag = { change, _ ->
                change.consume()
                val current = MouseInfo.getPointerInfo().location
                if (current != lastPointer) {
                    window.location = Point(
                        window.x + current.x - lastPointer.x,
                        window.y + current.y - lastPointer.y,
                    )
                    lastPointer = current
                }
            },
        )
    }
}
