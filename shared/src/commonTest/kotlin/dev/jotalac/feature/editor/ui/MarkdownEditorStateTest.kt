package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.jotalac.feature.editor.ui.components.active_block.EditorHistoryManager
import io.ktor.client.request.invoke
import org.intellij.markdown.lexer.Compat.assert
import org.koin.viewmodel.emptyState
import kotlin.collections.emptyList
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownEditorStateTest {

    private val blocksState = mutableStateOf(emptyList<String>())

    private val dispatchedActions = mutableListOf<EditorAction>()
    private val onActionState = mutableStateOf<(EditorAction) -> Unit>({ action ->
        dispatchedActions.add(action)
    })

    private lateinit var historyManager: EditorHistoryManager
    private lateinit var markdownEditorState: MarkdownEditorState

    @BeforeTest
    fun setup() {
        //clear all actions before each test
        dispatchedActions.clear()
        blocksState.value = listOf("first block", "second block")
        historyManager = EditorHistoryManager()

        markdownEditorState = MarkdownEditorState(
            blocksState = blocksState,
            historyManager = historyManager,
            onActionState = onActionState
        )
    }


    @Test
    fun `focusBlock sets the focusedIndex and activeTextFieldValue correctly`() {
        markdownEditorState.focusBlock(1)

        assertEquals(1, markdownEditorState.focusedIndex)
        assertEquals("second block", markdownEditorState.activeTextFieldValue.text)
        // makes sure the cursor is at the end of the text field
        assertEquals(blocksState.value[1].length, markdownEditorState.activeTextFieldValue.selection.start)
    }

    @Test
    fun `focusBlock respects the cursor TextRange when indexes are out of bounds`() {
        markdownEditorState.focusBlock(0, TextRange(0, 80))

        assertEquals(0, markdownEditorState.focusedIndex)
        assertEquals("first block", markdownEditorState.activeTextFieldValue.text)
        assertEquals(TextRange(0, blocksState.value[0].length), markdownEditorState.activeTextFieldValue.selection)
    }

    @Test
    fun `focusBlock focues unexisting index doesn't do anything`() {
        markdownEditorState.focusBlock(10)

        assertEquals(null, markdownEditorState.focusedIndex)
    }

    @Test
    fun `moving the caret without changing the text still reaches the field`() {
        markdownEditorState.focusBlock(0)

        // same text, new caret: what a click in the middle of the block produces
        markdownEditorState.updateActiveText(TextFieldValue("first block", TextRange(3)))

        assertEquals(TextRange(3), markdownEditorState.activeTextFieldValue.selection)
    }

    @Test
    fun `a caret move does not use up an undo step`() {
        markdownEditorState.focusBlock(0)
        markdownEditorState.updateActiveText(TextFieldValue("new text"))
        markdownEditorState.updateActiveText(TextFieldValue("new text", TextRange(2)))

        markdownEditorState.undo()

        assertEquals("first block", markdownEditorState.activeTextFieldValue.text)
    }

    @Test
    fun `updateActiveText updates the activeTextFieldValue correctly`() {
        val newTextValue = "new text"
        markdownEditorState.focusBlock(0)
        markdownEditorState.updateActiveText(TextFieldValue(newTextValue))

        assertEquals("new text", markdownEditorState.activeTextFieldValue.text)
    }

    @Test
    fun `updateActiveText doesn't update when it comes from different block index safety synchronization check`() {
        val newTextValue = "new text"
        markdownEditorState.focusBlock(0)
        markdownEditorState.updateActiveText(TextFieldValue(newTextValue), 10)

        assertEquals("first block", markdownEditorState.activeTextFieldValue.text)
    }

    @Test
    fun `undo text change`() {
        markdownEditorState.focusBlock(0)
        markdownEditorState.updateActiveText(TextFieldValue("new text"))
        markdownEditorState.undo()

        assertEquals("first block", markdownEditorState.activeTextFieldValue.text)
    }

    @Test
    fun `calling undo multiple times doesnt do anything`() {
        markdownEditorState.focusBlock(0)
        markdownEditorState.updateActiveText(TextFieldValue("new text"))
        markdownEditorState.undo()
        markdownEditorState.undo()
        markdownEditorState.undo()

        assertEquals("first block", markdownEditorState.activeTextFieldValue.text)
    }

    // moving between blocks

    @Test
    fun `moveUp puts the cursor at the end of the previous block`() {
        markdownEditorState.focusBlock(1)

        assertTrue(markdownEditorState.moveUp())
        assertEquals(0, markdownEditorState.focusedIndex)
        assertEquals(
            blocksState.value[0].length,
            markdownEditorState.activeTextFieldValue.selection.start,
        )
    }

    @Test
    fun `moveUp on the first block stays there and reports false`() {
        markdownEditorState.focusBlock(0)

        assertFalse(markdownEditorState.moveUp())
        assertEquals(0, markdownEditorState.focusedIndex)
    }

    @Test
    fun `moveDown puts the cursor at the end of the next block`() {
        markdownEditorState.focusBlock(0)

        assertTrue(markdownEditorState.moveDown())
        assertEquals(1, markdownEditorState.focusedIndex)
        assertEquals(
            blocksState.value[1].length,
            markdownEditorState.activeTextFieldValue.selection.start,
        )
    }

    @Test
    fun `moveDown on the last block stays there and reports false`() {
        markdownEditorState.focusBlock(1)

        assertFalse(markdownEditorState.moveDown())
        assertEquals(1, markdownEditorState.focusedIndex)
    }

    @Test
    fun `swapBlockUp reports the swap and keeps the moved block focused`() {
        markdownEditorState.focusBlock(1)

        markdownEditorState.swapBlockUp()

        assertIs<EditorAction.SwapBlocks>(dispatchedActions.last())
        assertEquals(0, markdownEditorState.focusedIndex)
    }

    @Test
    fun `swapBlockUp on the first block does nothing`() {
        markdownEditorState.focusBlock(0)

        markdownEditorState.swapBlockUp()

        assertTrue(dispatchedActions.none { it is EditorAction.SwapBlocks })
        assertEquals(0, markdownEditorState.focusedIndex)
    }

    @Test
    fun `swapBlockDown reports the swap and keeps the moved block focused`() {
        blocksState.value = listOf("a", "b", "c")
        markdownEditorState.focusBlock(1)

        markdownEditorState.swapBlockDown()

        assertIs<EditorAction.SwapBlocks>(dispatchedActions.last())
        assertEquals(2, markdownEditorState.focusedIndex)
    }

    // removing and merging blocks

    @Test
    fun `backspaceOnEmpty removes the block and moves the cursor to the end of the previous one`() {
        markdownEditorState.focusBlock(1)

        assertTrue(markdownEditorState.backspaceOnEmpty())

        assertIs<EditorAction.RemoveBlock>(dispatchedActions.last())
        assertEquals(0, markdownEditorState.focusedIndex)
        assertEquals(
            blocksState.value[0].length,
            markdownEditorState.activeTextFieldValue.selection.start,
        )
    }

    @Test
    fun `backspaceOnStart in the first block keeps the block`() {
        markdownEditorState.focusBlock(0)

        assertTrue(markdownEditorState.backspaceOnStart())

        assertTrue(dispatchedActions.none { it is EditorAction.MergeWithPrevBlock })
        assertEquals(0, markdownEditorState.focusedIndex)
    }

    @Test
    fun `backspaceOnStart merges into the previous block with the cursor at the join`() {
        markdownEditorState.focusBlock(1)

        assertTrue(markdownEditorState.backspaceOnStart())

        assertIs<EditorAction.MergeWithPrevBlock>(dispatchedActions.last())
        assertEquals(0, markdownEditorState.focusedIndex)
        assertEquals(
            blocksState.value[0].length,
            markdownEditorState.activeTextFieldValue.selection.start,
        )
    }

    @Test
    fun `addBlockAtEndIfNotEmpty reuses a trailing empty block`() {
        blocksState.value = listOf("first block", "")
        markdownEditorState.focusBlock(0)

        markdownEditorState.addBlockAtEndIfNotEmpty()

        assertTrue(dispatchedActions.none { it is EditorAction.AddBlock })
        assertEquals(1, markdownEditorState.focusedIndex)
    }

    @Test
    fun `addBlockAtEndIfNotEmpty adds a block when the last one has text`() {
        markdownEditorState.focusBlock(0)

        markdownEditorState.addBlockAtEndIfNotEmpty()

        assertIs<EditorAction.AddBlock>(dispatchedActions.last())
    }

    // focus lost

    @Test
    fun `evaluateFocusLost clears the focus and reports which block lost it`() {
        markdownEditorState.focusBlock(1)

        markdownEditorState.evaluateFocusLost(1)

        assertNull(markdownEditorState.focusedIndex)
        val action = assertIs<EditorAction.EvaluateBlockOnFocusLost>(dispatchedActions.last())
        assertEquals(1, action.index)
        assertNull(action.currentFocusedIndex)
    }

    @Test
    fun `evaluateFocusLost of another block keeps the current focus`() {
        markdownEditorState.focusBlock(1)

        markdownEditorState.evaluateFocusLost(0)

        assertEquals(1, markdownEditorState.focusedIndex)
        val action = assertIs<EditorAction.EvaluateBlockOnFocusLost>(dispatchedActions.last())
        assertEquals(0, action.index)
        assertEquals(1, action.currentFocusedIndex)
    }

    @Test
    fun `evaluateFocusLost can move the focus to an adjusted block`() {
        markdownEditorState.focusBlock(1)

        markdownEditorState.evaluateFocusLost(1)
        val action = assertIs<EditorAction.EvaluateBlockOnFocusLost>(dispatchedActions.last())
        action.onFocusAdjusted(0)

        assertEquals(0, markdownEditorState.focusedIndex)
    }

    @Test
    fun `splitBlock reports the split to the platform`() {
        markdownEditorState.focusBlock(0)

        markdownEditorState.splitBlock(cursorStart = 3)

        assertIs<EditorAction.SplitBlock>(dispatchedActions.last())
    }

    @Test
    fun `redo without anything to redo does nothing`() {
        markdownEditorState.focusBlock(0)

        markdownEditorState.redo()

        assertEquals(0, markdownEditorState.focusedIndex)
        assertEquals("first block", markdownEditorState.activeTextFieldValue.text)
    }
}