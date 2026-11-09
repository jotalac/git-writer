package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import dev.jotalac.feature.editor.ui.EditorAction
import dev.jotalac.feature.editor.ui.EditorHistoryManager
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
* A click into a note scrolled away from its cursor, while the field is not focused yet - the click puts the cursor
* where it is, and the note stays where it is scrolled.
* */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
class LivePreviewClickScrollTest {

    private val text = (1..80).joinToString("\n\n") { "line number $it" }
    private val noteText = mutableStateOf(text)
    private val editorState = MarkdownEditorState(
        noteTextState = noteText,
        onActionState = mutableStateOf({ action -> if (action is EditorAction.UpdateText) noteText.value = action.text }),
        historyManager = EditorHistoryManager(),
    )
    private val scrollState = ScrollState(0)
    private val scene = ImageComposeScene(width = 700, height = 500, density = Density(1f)) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().verticalScroll(scrollState)) { LivePreviewField(editorState) }
            }
        }
    }
    private var time = 0L

    @AfterTest
    fun close() = scene.close()

    private fun frames(count: Int) = repeat(count) {
        scene.render(time)
        time += 16_000_000
    }

    // clicks the last line, near the bottom of the window, of the note scrolled to its end
    private fun assertClickKeepsScroll(framesWhilePressed: Int) {
        frames(10)
        runBlocking { scrollState.scrollTo(scrollState.maxValue) }
        frames(5)
        val scrolled = scrollState.value
        assertTrue(scrolled > 0)

        val position = Offset(60f, 480f)
        scene.sendPointerEvent(PointerEventType.Move, position)
        scene.sendPointerEvent(PointerEventType.Press, position)
        frames(framesWhilePressed)
        scene.sendPointerEvent(PointerEventType.Release, position)
        frames(10)

        assertEquals(scrolled, scrollState.value, "scrolled away")
        assertTrue(editorState.value.selection.start > text.length - 30, "cursor at ${editorState.value.selection}")
    }

    @Test
    fun `a click below the cursor puts the cursor there without scrolling`() = assertClickKeepsScroll(framesWhilePressed = 5)

    // the press and the release handled before the focus change's own work runs
    @Test
    fun `a quick click does not scroll either`() = assertClickKeepsScroll(framesWhilePressed = 0)

    // a click on an image, a formula or the space under the note focuses the field from the code
    @Test
    fun `focusing at an offset in view does not scroll`() {
        frames(10)
        runBlocking { scrollState.scrollTo(scrollState.maxValue) }
        frames(5)
        val scrolled = scrollState.value

        editorState.focusAt(text.length - 5)
        repeat(10) {
            frames(1)
            assertEquals(scrolled, scrollState.value, "scrolled away in frame $it")
        }
        assertTrue(editorState.isFocused)
    }
}
