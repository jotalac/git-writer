package dev.jotalac.feature.editor.ui.components

import androidx.compose.ui.text.TextRange
import kotlin.time.Clock

data class EditorHistoryItem(
    val blocks: List<String>,
    val focusedIndex: Int?,
    val selection: TextRange? = null,
)

class EditorHistoryManager(
    private val maxHistorySize: Int = 50,
    private val typingDebounceMillis: Long = 500L,
) {

    private val undoStack = ArrayDeque<EditorHistoryItem>()
    private val redoStack = ArrayDeque<EditorHistoryItem>()

    private var lastRecordTime: Long = 0L

    fun record(item: EditorHistoryItem, isImmediate: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        val isConsecutiveTyping = !isImmediate && (now - lastRecordTime < typingDebounceMillis)
        lastRecordTime = now

        if (isConsecutiveTyping && undoStack.isNotEmpty()) {
            return
        }

        if (undoStack.lastOrNull() == item) return

        undoStack.addLast(item)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }

        redoStack.clear()
    }


    fun undo(currentState: EditorHistoryItem): EditorHistoryItem? {
        val previousState = undoStack.removeLastOrNull() ?: return null
        lastRecordTime = 0L

        redoStack.addLast(currentState)
        if (redoStack.size > maxHistorySize) {
            redoStack.removeFirst()
        }

        return previousState

    }

    fun redo(currentState: EditorHistoryItem): EditorHistoryItem? {
        val nextState = redoStack.removeLastOrNull() ?: return null
        lastRecordTime = 0L

        undoStack.addLast(currentState)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }

        return nextState
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastRecordTime = 0L
    }

}