package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownEditorStateTest {

    private val noteText = mutableStateOf("")

    private val dispatchedActions = mutableListOf<EditorAction>()
    private val onActionState = mutableStateOf<(EditorAction) -> Unit>({ action ->
        dispatchedActions.add(action)
        // the view model keeps the text, like EditorViewModel does
        if (action is EditorAction.UpdateText) noteText.value = action.text
    })

    private lateinit var editorState: MarkdownEditorState

    @BeforeTest
    fun setup() {
        dispatchedActions.clear()
        noteText.value = "first line\nsecond line"
        // every edit is its own undo step
        editorState = MarkdownEditorState(noteText, onActionState, EditorHistoryManager(typingDebounceMillis = 0))
    }

    private fun typed(text: String, cursor: Int = text.length) = TextFieldValue(text, TextRange(cursor))

    private fun lastText() = assertIs<EditorAction.UpdateText>(dispatchedActions.last()).text

    @Test
    fun `the editor starts with the note's text`() {
        assertEquals("first line\nsecond line", editorState.value.text)
    }

    @Test
    fun `an edit is sent to the view model`() {
        editorState.onValueChange(typed("first line!\nsecond line", cursor = 11))

        assertEquals("first line!\nsecond line", lastText())
        assertEquals(TextRange(11), editorState.value.selection)
    }

    @Test
    fun `moving the cursor sends nothing and uses no undo step`() {
        editorState.onValueChange(typed("first line\nsecond line", cursor = 3))

        assertTrue(dispatchedActions.isEmpty())
        assertEquals(TextRange(3), editorState.value.selection)

        editorState.undo()
        assertTrue(dispatchedActions.isEmpty())
    }

    @Test
    fun `undo and redo go back and forth with the cursor`() {
        editorState.onValueChange(typed("first line\nsecond line", cursor = 10))
        editorState.onValueChange(typed("first line!\nsecond line", cursor = 11))

        editorState.undo()
        assertEquals("first line\nsecond line", lastText())
        assertEquals(TextRange(10), editorState.value.selection)

        editorState.redo()
        assertEquals("first line!\nsecond line", lastText())
        assertEquals(TextRange(11), editorState.value.selection)
    }

    @Test
    fun `a new line in a list continues the list`() {
        noteText.value = "- milk"
        editorState.syncExternalText()

        // what the text field gives for Enter (or a mobile keyboard) at the end
        editorState.onValueChange(typed("- milk\n"))

        assertEquals("- milk\n- ", lastText())
        assertEquals(TextRange(9), editorState.value.selection)
    }

    @Test
    fun `a new line in code is only a new line`() {
        noteText.value = "```\n- milk"
        editorState.syncExternalText()

        editorState.onValueChange(typed("```\n- milk\n"))

        assertEquals("```\n- milk\n", lastText())
    }

    @Test
    fun `replacing a range keeps the cursor on the same text`() {
        noteText.value = "- [ ] milk\nbread"
        editorState.syncExternalText()
        editorState.onValueChange(typed("- [ ] milk\nbread", cursor = 13))

        editorState.replaceRange(3, 4, "x")

        assertEquals("- [x] milk\nbread", lastText())
        assertEquals(TextRange(13), editorState.value.selection)
    }

    @Test
    fun `a text changed outside the editor keeps the cursor where it can be`() {
        editorState.onValueChange(typed("first line\nsecond line", cursor = 20))

        noteText.value = "short"
        editorState.syncExternalText()

        assertEquals("short", editorState.value.text)
        assertEquals(TextRange(5), editorState.value.selection)
    }

    @Test
    fun `selecting focuses the text field with the selection inside the text`() {
        var focusRequests = 0
        editorState.requestFieldFocus = { focusRequests++ }

        editorState.select(TextRange(6, 100))

        assertEquals(TextRange(6, 22), editorState.value.selection)
        assertEquals(1, focusRequests)
    }

    @Test
    fun `pasted images go after the line with the cursor once they are saved`() {
        editorState.onValueChange(typed("first line\nsecond line", cursor = 3))

        editorState.pasteImages(listOf(byteArrayOf(1)))
        val paste = assertIs<EditorAction.PasteImages>(dispatchedActions.last())
        paste.onImagesSaved(listOf("![pasted image](images/a.png)"))

        assertEquals("first line\n\n![pasted image](images/a.png)\n\nsecond line", lastText())
    }

    @Test
    fun `bold wraps the selection`() {
        editorState.onValueChange(TextFieldValue("first line\nsecond line", TextRange(0, 5)))

        editorState.applyBold()

        assertEquals("**first** line\nsecond line", lastText())
    }

    @Test
    fun `lines move up and down with the cursor`() {
        editorState.onValueChange(typed("first line\nsecond line", cursor = 13))

        editorState.moveLinesUp()
        assertEquals("second line\nfirst line", lastText())
        assertEquals(TextRange(2), editorState.value.selection)

        editorState.moveLinesDown()
        assertEquals("first line\nsecond line", lastText())
        assertEquals(TextRange(13), editorState.value.selection)
    }

    @Test
    fun `shift enter breaks the line without continuing the list`() {
        noteText.value = "- milk"
        editorState.syncExternalText()
        editorState.onValueChange(typed("- milk"))

        editorState.insertPlainNewLine()

        assertEquals("- milk\n", lastText())
    }
}
