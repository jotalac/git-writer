package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import dev.jotalac.feature.editor.ui.EditorAction
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import dev.jotalac.feature.editor.ui.EditorHistoryManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/*
* The cursor keys that move by the lines on screen, in the editor rendered offscreen - the empty lines around
* headings hold a zero width space in place of their line break, which the moves must not get stuck on.
* */
@OptIn(InternalComposeUiApi::class)
class LivePreviewCursorKeysTest {

    private val text = "- a list item\n\n\n# heading\ntext under it"
    private val noteText = mutableStateOf(text)
    private val editorState = MarkdownEditorState(
        noteTextState = noteText,
        onActionState = mutableStateOf({ action -> if (action is EditorAction.UpdateText) noteText.value = action.text }),
        historyManager = EditorHistoryManager(),
    )
    private val scene = ImageComposeScene(width = 700, height = 500, density = Density(1.5f)) {
        MaterialTheme { Surface(modifier = Modifier.fillMaxSize()) { LivePreviewField(editorState) } }
    }
    private var time = 0L

    @AfterTest
    fun close() = scene.close()

    private fun frames(count: Int) = repeat(count) {
        scene.render(time)
        time += 16_000_000
    }

    private fun cursorAt(offset: Int) {
        frames(10)
        editorState.select(TextRange(offset))
        frames(10)
    }

    private fun press(key: Key): Int {
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
        frames(2)
        return editorState.value.selection.start
    }

    private val headingStart = text.indexOf("# heading")

    @Test
    fun `up from the start of a heading goes to the empty lines above it`() {
        cursorAt(headingStart)

        assertEquals(headingStart - 1, press(Key.DirectionUp))
        assertEquals(headingStart - 2, press(Key.DirectionUp))
        // in the column of the start of the heading
        assertEquals(0, press(Key.DirectionUp), "the start of the list item")
    }

    @Test
    fun `down goes through the empty lines into the heading and past it`() {
        cursorAt(0)

        assertEquals(listOf(headingStart - 2, headingStart - 1), listOf(press(Key.DirectionDown), press(Key.DirectionDown)))
        val inHeading = press(Key.DirectionDown)
        assertEquals("# heading", text.substring(text.lastIndexOf('\n', inHeading - 1) + 1).substringBefore('\n'))
        press(Key.DirectionDown)
        assertEquals("text under it", text.substring(text.lastIndexOf('\n', editorState.value.selection.start - 1) + 1))
    }

    @Test
    fun `end stays on the heading's line`() {
        cursorAt(headingStart + 3)

        assertEquals(text.indexOf("\ntext under it"), press(Key.MoveEnd))
    }
}
