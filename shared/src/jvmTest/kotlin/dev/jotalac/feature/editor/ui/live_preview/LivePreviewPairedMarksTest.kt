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
import dev.jotalac.feature.editor.ui.EditorHistoryManager
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import java.awt.Component
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/*
* A typed character comes as a key down and then once more as a typed event (AWT's KEY_TYPED), which the text field
* types - for a paired mark the editor types itself, the field must not type it once more.
* */
@OptIn(InternalComposeUiApi::class)
class LivePreviewPairedMarksTest {

    private val noteText = mutableStateOf("snake")
    private val editorState = MarkdownEditorState(
        noteTextState = noteText,
        onActionState = mutableStateOf({ action -> if (action is EditorAction.UpdateText) noteText.value = action.text }),
        historyManager = EditorHistoryManager(),
    )
    private val scene = ImageComposeScene(width = 700, height = 500, density = Density(1f)) {
        MaterialTheme { Surface(modifier = Modifier.fillMaxSize()) { LivePreviewField(editorState) } }
    }
    private var time = 0L
    private val source = object : Component() {}

    @AfterTest
    fun close() = scene.close()

    private fun frames(count: Int) = repeat(count) {
        scene.render(time)
        time += 16_000_000
    }

    private fun type(key: Key, char: Char) {
        val now = System.currentTimeMillis()
        val pressed = java.awt.event.KeyEvent(source, java.awt.event.KeyEvent.KEY_PRESSED, now, 0, 0, char)
        val typed = java.awt.event.KeyEvent(source, java.awt.event.KeyEvent.KEY_TYPED, now, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown, char.code, nativeEvent = pressed))
        scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, char.code, nativeEvent = typed))
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp, char.code))
        frames(2)
    }

    private fun shown(): String {
        val value = editorState.value
        return value.text.substring(0, value.selection.start) + "|" + value.text.substring(value.selection.start)
    }

    @Test
    fun `a mark after a space types its pair once, and the closing one is stepped over`() {
        frames(10)
        editorState.select(TextRange(5))
        frames(10)

        type(Key.Spacebar, ' ')
        type(Key.Grave, '`')
        assertEquals("snake `|`", shown())
        type(Key.A, 'a')
        type(Key.Grave, '`')
        assertEquals("snake `a`|", shown())
    }

    @Test
    fun `a mark in the middle of a word is typed by the field`() {
        frames(10)
        editorState.select(TextRange(5))
        frames(10)

        type(Key.Minus, '_')
        assertEquals("snake_|", shown())
    }
}
