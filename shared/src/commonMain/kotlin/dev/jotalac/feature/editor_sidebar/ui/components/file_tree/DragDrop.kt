package dev.jotalac.feature.editor_sidebar.ui.components.file_tree

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode

class DragDropState {
    var isDragging by mutableStateOf(false)
    var draggedNode by mutableStateOf<FlatFileNode?>(null)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffsetWithinNode by mutableStateOf(Offset.Zero)
    
    var dropTargetResolver: (() -> String?)? = null

    fun onDragStart(node: FlatFileNode, position: Offset, offsetWithinNode: Offset) {
        isDragging = true
        draggedNode = node
        dragPosition = position
        dragOffsetWithinNode = offsetWithinNode
    }

    fun onDrag(position: Offset) {
        dragPosition = position
    }

    fun onDragEnd(): String? {
        val target = dropTargetResolver?.invoke()
        isDragging = false
        draggedNode = null
        return target
    }

    fun onDragCancel() {
        isDragging = false
        draggedNode = null
    }
}

val LocalDragDropState = compositionLocalOf { DragDropState() }

fun Modifier.dragSource(
    node: FlatFileNode,
    dragDropState: DragDropState,
    onDragEnd: (String?) -> Unit
): Modifier = composed {
    var globalPosition by remember { mutableStateOf(Offset.Zero) }
    
    this.onGloballyPositioned { coordinates ->
        globalPosition = coordinates.boundsInWindow().topLeft
    }.pointerInput(node) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            
            suspend fun AwaitPointerEventScope.handleDrag(startChange: PointerInputChange) {
                dragDropState.onDragStart(node, globalPosition + startChange.position, startChange.position)
                drag(startChange.id) { change ->
                    dragDropState.onDrag(globalPosition + change.position)
                    change.consume()
                }
                val targetPath = dragDropState.onDragEnd()
                onDragEnd(targetPath)
            }
            
            if (down.type == PointerType.Mouse) {
                var dragEvent: PointerInputChange? = null
                awaitTouchSlopOrCancellation(down.id) { change, _ ->
                    dragEvent = change
                    change.consume()
                }
                if (dragEvent != null) {
                    handleDrag(dragEvent!!)
                }
            } else {
                val longPress = awaitLongPressOrCancellation(down.id)
                if (longPress != null) {
                    handleDrag(longPress)
                }
            }
        }
    }
}
