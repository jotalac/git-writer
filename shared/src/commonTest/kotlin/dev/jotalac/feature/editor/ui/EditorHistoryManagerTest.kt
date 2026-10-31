package dev.jotalac.feature.editor.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/*
* The manager reads the real clock for its typing debounce, so the tests pick the window instead of
* controlling time: 0 ms makes every record its own undo step, 1 hour makes a test-fast burst of
* records count as one typing session.
* */
class EditorHistoryManagerTest {

    private val oneHour = 60 * 60 * 1000L

    private fun state(vararg lines: String) = EditorHistoryItem(lines.joinToString("\n"))

    @Test
    fun `undo walks back through the recorded states`() {
        val history = EditorHistoryManager(typingDebounceMillis = 0)
        history.record(state("a"))
        history.record(state("a", "b"))

        assertEquals(state("a", "b"), history.undo(currentState = state("a", "b", "c")))
        assertEquals(state("a"), history.undo(currentState = state("a", "b")))
        assertNull(history.undo(currentState = state("a")))
    }

    @Test
    fun `redo returns to the state before the undo`() {
        val history = EditorHistoryManager(typingDebounceMillis = 0)
        history.record(state("before"))

        history.undo(currentState = state("after"))

        assertEquals(state("after"), history.redo(currentState = state("before")))
    }

    @Test
    fun `a new edit after undo drops the redo history`() {
        val history = EditorHistoryManager(typingDebounceMillis = 0)
        history.record(state("a"))
        history.undo(currentState = state("b"))

        history.record(state("edited"))

        assertNull(history.redo(currentState = state("edited")))
    }

    @Test
    fun `a burst of typing is undone in one step`() {
        val history = EditorHistoryManager(typingDebounceMillis = oneHour)
        history.record(state("h"))
        history.record(state("he"))
        history.record(state("hel"))

        assertEquals(state("h"), history.undo(currentState = state("hell")))
        assertNull(history.undo(currentState = state("h")))
    }

    @Test
    fun `immediate changes are their own undo step even while typing`() {
        // e.g. a formatting shortcut in the middle of typing
        val history = EditorHistoryManager(typingDebounceMillis = oneHour)
        history.record(state("text"))
        history.record(state("text", "new block"), isImmediate = true)

        assertEquals(state("text", "new block"), history.undo(currentState = state("text", "new block!")))
        assertEquals(state("text"), history.undo(currentState = state("text", "new block")))
    }

    @Test
    fun `typing after an undo starts a new undo step`() {
        val history = EditorHistoryManager(typingDebounceMillis = oneHour)
        history.record(state("a"))
        history.record(state("a", "b"), isImmediate = true)
        history.undo(currentState = state("a", "b", "c"))

        // still inside the typing window, but the undo ended the previous typing session
        history.record(state("a", "x"))

        assertEquals(state("a", "x"), history.undo(currentState = state("a", "xy")))
    }

    @Test
    fun `recording the same state twice keeps one undo step`() {
        val history = EditorHistoryManager(typingDebounceMillis = 0)
        history.record(state("same"))
        history.record(state("same"))

        assertEquals(state("same"), history.undo(currentState = state("changed")))
        assertNull(history.undo(currentState = state("same")))
    }

    @Test
    fun `the oldest states are dropped when the history is full`() {
        val history = EditorHistoryManager(maxHistorySize = 3, typingDebounceMillis = 0)
        (1..5).forEach { history.record(state("$it")) }

        assertEquals(state("5"), history.undo(currentState = state("6")))
        assertEquals(state("4"), history.undo(currentState = state("5")))
        assertEquals(state("3"), history.undo(currentState = state("4")))
        assertNull(history.undo(currentState = state("3")))
    }

    @Test
    fun `clear forgets both undo and redo`() {
        // used when another note is opened - its history must not apply to the new note
        val history = EditorHistoryManager(typingDebounceMillis = 0)
        history.record(state("a"))
        history.record(state("b"))
        history.undo(currentState = state("c"))

        history.clear()

        assertNull(history.undo(currentState = state("x")))
        assertNull(history.redo(currentState = state("x")))
    }
}
