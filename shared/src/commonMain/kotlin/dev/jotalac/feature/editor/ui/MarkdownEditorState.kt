package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.jotalac.feature.editor.ui.utils.addLink
import dev.jotalac.feature.editor.ui.utils.applyBold
import dev.jotalac.feature.editor.ui.utils.applyInlineCode
import dev.jotalac.feature.editor.ui.utils.applyItalic

@Stable
class MarkdownEditorState(
    private val blocksState: State<List<String>>,
    private val onActionState: State<(EditorAction) -> Unit>
) {
    var focusedIndex by mutableStateOf<Int?>(null)
        private set

    var activeTextFieldValue by mutableStateOf(TextFieldValue())
        private set

    fun focusBlock(index: Int, cursor: TextRange? = null) {
        focusedIndex = index
        val text = blocksState.value.getOrNull(index) ?: ""
        activeTextFieldValue = TextFieldValue(
            text = text,
            selection = cursor ?: TextRange(text.length)
        )
    }

    fun clearFocus() {
        focusedIndex = null
    }

    fun updateActiveText(newValue: TextFieldValue) {
        activeTextFieldValue = newValue
        focusedIndex?.let { index ->
            if (blocksState.value.getOrNull(index) != newValue.text) {
                dispatchAction(EditorAction.UpdateBlock(index, newValue.text))
            }
        }
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

    // block actions

    fun evaluateFocusLost(index: Int, text: String) {
        dispatchAction(
            EditorAction.EvaluateBlockOnFocusLost(
                index = index,
                currentFocusedIndex = focusedIndex,
                onFocusAdjusted = { focusBlock(index) }
            )
        )
    }

    fun addBlockBelow(index: Int) {
        dispatchAction(EditorAction.AddBlock(index + 1))
        focusBlock(index + 1, TextRange(0))
    }

    fun splitBlock(cursorStart: Int) {
        val index = focusedIndex ?: return
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

    fun swapBlockUp() {
        val index = focusedIndex ?: return
        if (index > 0) {
            val targetIndex = index - 1
            dispatchAction(EditorAction.SwapBlocks(index, targetIndex))
            focusBlock(targetIndex, activeTextFieldValue.selection)
        }
    }

    fun swapBlockDown() {
        val index = focusedIndex ?: return
        if (index < blocksState.value.lastIndex) {
            val targetIndex = index + 1
            dispatchAction(EditorAction.SwapBlocks(index, targetIndex))
            focusBlock(targetIndex, activeTextFieldValue.selection)
        }
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

    fun backspaceOnEmpty(): Boolean {
        val index = focusedIndex ?: return false
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

        val originalBlockLength = blocksState.value.getOrNull(index - 1)?.length ?: 0
        dispatchAction(EditorAction.MergeWithPrevBlock(index))
        focusBlock(index - 1, TextRange(originalBlockLength))
        return true
    }

    fun addBlockAtEnd() {
        val newIndex = blocksState.value.lastIndex + 1
        dispatchAction(EditorAction.AddBlock())
        focusBlock(newIndex, TextRange(0))
    }

    fun deleteBlock(index: Int) {
        dispatchAction(EditorAction.RemoveBlock(index))
        if (focusedIndex == index) {
            clearFocus()
        }
    }

    fun pasteImages(imageBytesList: List<ByteArray>) {
        val index = focusedIndex ?: return
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
        updateActiveText(activeTextFieldValue.applyBold())
    }

    fun applyItalic() {
        updateActiveText(activeTextFieldValue.applyItalic())
    }

    fun addLinkTemplate() {
        updateActiveText(activeTextFieldValue.addLink())
    }

    fun applyInlineCode() {
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

    val state = remember { MarkdownEditorState(blocksState, onActionState) }

    LaunchedEffect(state.focusedIndex, blocks) {
        state.syncExternalBlocks()
    }

    return state
}
