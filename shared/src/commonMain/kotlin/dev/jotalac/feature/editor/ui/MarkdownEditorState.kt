package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.jotalac.feature.editor.ui.components.active_block.EditorHistoryItem
import dev.jotalac.feature.editor.ui.components.active_block.EditorHistoryManager
import dev.jotalac.feature.editor.ui.utils.addLink
import dev.jotalac.feature.editor.ui.utils.applyBold
import dev.jotalac.feature.editor.ui.utils.applyInlineCode
import dev.jotalac.feature.editor.ui.utils.applyItalic

@Stable
class MarkdownEditorState(
    private val blocksState: State<List<String>>,
    private val onActionState: State<(EditorAction) -> Unit>,
    private val historyManager: EditorHistoryManager
) {
    var focusedIndex by mutableStateOf<Int?>(null)
        private set

    var activeTextFieldValue by mutableStateOf(TextFieldValue())
        private set


    var requestRootFocus: (() -> Unit)? = null

    fun requestEditorFocus() {
        requestRootFocus?.invoke()
    }

    fun focusBlock(index: Int, cursor: TextRange? = null) {
        focusedIndex = index
        val text = blocksState.value.getOrNull(index) ?: ""
        val safeCursor = if (cursor != null) {
            TextRange(
                cursor.start.coerceIn(0, text.length),
                cursor.end.coerceIn(0, text.length)
            )
        } else {
            TextRange(text.length)
        }
        activeTextFieldValue = TextFieldValue(
            text = text,
            selection = safeCursor
        )
    }

    fun clearFocus() {
        focusedIndex = null
        requestEditorFocus()
    }

    fun updateActiveText(newValue: TextFieldValue, fromIndex: Int? = null) {
        //only update the block when it came from the actual block - handles synchronization errors
        if (fromIndex != null && fromIndex != focusedIndex) return

        historyManager.record(createCurrentSnapshot())

        activeTextFieldValue = newValue
        focusedIndex?.let { index ->
            if (blocksState.value.getOrNull(index) != newValue.text) {
                dispatchAction(EditorAction.UpdateBlock(index, newValue.text))
            }
        }

    }

    fun updateBlockText(index: Int, newText: String) {
        if (index !in blocksState.value.indices) return
        historyManager.record(createCurrentSnapshot(), true)
        dispatchAction(EditorAction.UpdateBlock(index, newText))
    }

    fun dispatchAction(action: EditorAction) {
        onActionState.value(action)
    }

    fun syncExternalBlocks() {
        val index = focusedIndex ?: return
        val currentText = blocksState.value.getOrNull(index) ?: return

        if (activeTextFieldValue.text != currentText) {
            activeTextFieldValue = activeTextFieldValue.copy(
                text = currentText,
                selection = TextRange(
                    activeTextFieldValue.selection.start.coerceIn(0, currentText.length),
                    activeTextFieldValue.selection.end.coerceIn(0, currentText.length)
                )
            )
        }
    }

    // undo/redo logic
    private fun createCurrentSnapshot(): EditorHistoryItem {
        return EditorHistoryItem(
            blocks = blocksState.value.toList(),
            focusedIndex = focusedIndex,
            selection = activeTextFieldValue.selection,
        )
    }

    fun undo() {
        val prevState = historyManager.undo(createCurrentSnapshot()) ?: return

        dispatchAction(EditorAction.SetBlocks(prevState.blocks))
        if (prevState.focusedIndex != null && prevState.focusedIndex in prevState.blocks.indices) {
            focusBlock(prevState.focusedIndex, prevState.selection)
        } else {
            clearFocus()
        }
    }

    fun redo() {
        val nextState = historyManager.redo(createCurrentSnapshot()) ?: return

        dispatchAction(EditorAction.SetBlocks(nextState.blocks))
        if (nextState.focusedIndex != null && nextState.focusedIndex in nextState.blocks.indices) {
            focusBlock(nextState.focusedIndex, nextState.selection)
        } else {
            clearFocus()
        }
    }

    // block actions

    fun evaluateFocusLost(index: Int) {
        if (index == focusedIndex) {
            focusedIndex = null
        }
        dispatchAction(
            EditorAction.EvaluateBlockOnFocusLost(
                index = index,
                currentFocusedIndex = focusedIndex,
                onFocusAdjusted = { adjustedIndex -> focusBlock(adjustedIndex ?: index) }
            )
        )
    }

    fun addBlockBelow(index: Int) {
        historyManager.record(createCurrentSnapshot(), true)

        dispatchAction(EditorAction.AddBlock(index + 1))
        focusBlock(index + 1, TextRange(0))
    }

    fun splitBlock(cursorStart: Int) {
        val index = focusedIndex ?: return

        historyManager.record(createCurrentSnapshot(), true)

        dispatchAction(EditorAction.SplitBlock(index, cursorStart) { newFocusIndex ->
            focusBlock(newFocusIndex, TextRange(0))
        })
    }

    fun moveUp(): Boolean {
        val index = focusedIndex ?: return false
        if (index > 0) {
            val previousText = blocksState.value.getOrNull(index - 1) ?: ""
            focusBlock(index - 1, TextRange(previousText.length))
            return true
        }
        return false
    }

    fun moveDown(): Boolean {
        val index = focusedIndex ?: return false
        if (index < blocksState.value.size - 1) {
            val nextText = blocksState.value.getOrNull(index + 1) ?: ""
            focusBlock(index + 1, TextRange(nextText.length))
            return true
        }
        return false
    }


    fun swapBlockUp(blockIndex: Int? = null) {
        val index = blockIndex ?: focusedIndex ?: return
        if (index > 0) {
            historyManager.record(createCurrentSnapshot(), true)

            val targetIndex = index - 1
            dispatchAction(EditorAction.SwapBlocks(index, targetIndex))

            // if the block was swapped from modal bottom sheet dont focus it
            focusedIndex?.let {
                focusBlock(targetIndex, activeTextFieldValue.selection)
            }
        }
    }

    fun swapBlockDown(blockIndex: Int? = null) {
        val index = blockIndex ?: focusedIndex ?: return
        if (index < blocksState.value.lastIndex) {
            historyManager.record(createCurrentSnapshot(), true)

            val targetIndex = index + 1
            dispatchAction(EditorAction.SwapBlocks(index, targetIndex))

            // if the block was swapped from modal bottom sheet dont focus it
            focusedIndex?.let {
                focusBlock(targetIndex, activeTextFieldValue.selection)
            }
        }
    }

    fun backspaceOnEmpty(): Boolean {
        val index = focusedIndex ?: return false

        historyManager.record(createCurrentSnapshot(), true)

        dispatchAction(EditorAction.RemoveBlock(index))

        val blocks = blocksState.value
        if (blocks.isNotEmpty()) {
            val newIndex = maxOf(0, index - 1)
            val text = blocks.getOrNull(newIndex) ?: ""
            focusBlock(newIndex, TextRange(text.length))
        } else {
            clearFocus()
        }
        return true
    }

    fun backspaceOnStart(): Boolean {
        val index = focusedIndex ?: return false
        if (index <= 0) return true

        historyManager.record(createCurrentSnapshot(), true)

        val originalBlockLength = blocksState.value.getOrNull(index - 1)?.length ?: 0
        dispatchAction(EditorAction.MergeWithPrevBlock(index))
        focusBlock(index - 1, TextRange(originalBlockLength))

        return true
    }

    fun addBlockAtEnd() {
        historyManager.record(createCurrentSnapshot(), true)

        val newIndex = blocksState.value.lastIndex + 1
        dispatchAction(EditorAction.AddBlock())
        focusBlock(newIndex, TextRange(0))
    }

    fun deleteBlock(index: Int) {
        historyManager.record(createCurrentSnapshot(), true)

        dispatchAction(EditorAction.RemoveBlock(index))
        val current = focusedIndex
        if (current == index) {
            clearFocus()
        } else if (current != null && current > index) {
            focusedIndex = current - 1
        } else if (current == null) {
            requestEditorFocus()
        }
    }

    fun pasteImages(imageBytesList: List<ByteArray>, targetIndex: Int? = null) {
        if (imageBytesList.isEmpty()) return
        val index = targetIndex ?: focusedIndex ?: maxOf(0, blocksState.value.lastIndex)
        historyManager.record(createCurrentSnapshot(), true)

        dispatchAction(
            EditorAction.PasteImages(
                imageBytesList = imageBytesList,
                focusedIndex = index,
                onFocusCalculated = { newFocusIndex ->
                    focusBlock(newFocusIndex, TextRange(0))
                }
            )
        )
    }


    fun handleEscape() {
        clearFocus()
    }

    fun applyBold() {
        historyManager.record(createCurrentSnapshot(), true)
        updateActiveText(activeTextFieldValue.applyBold())
    }

    fun applyItalic() {
        historyManager.record(createCurrentSnapshot(), true)
        updateActiveText(activeTextFieldValue.applyItalic())
    }

    fun addLinkTemplate() {
        historyManager.record(createCurrentSnapshot(), true)
        updateActiveText(activeTextFieldValue.addLink())
    }

    fun applyInlineCode() {
        historyManager.record(createCurrentSnapshot(), true)
        updateActiveText(activeTextFieldValue.applyInlineCode())
    }
}

@Composable
fun rememberMarkdownEditorState(
    blocks: List<String>,
    onAction: (EditorAction) -> Unit
): MarkdownEditorState {
    val blocksState = rememberUpdatedState(blocks)
    val onActionState = rememberUpdatedState(onAction)
    val historyManager = remember { EditorHistoryManager() }

    val state = remember { MarkdownEditorState(blocksState, onActionState, historyManager) }

    LaunchedEffect(state.focusedIndex, blocks) {
        state.syncExternalBlocks()
    }

    return state
}
