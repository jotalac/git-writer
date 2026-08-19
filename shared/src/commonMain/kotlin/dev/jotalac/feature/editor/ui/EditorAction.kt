package dev.jotalac.feature.editor.ui

sealed interface EditorAction {
    data class AddBlock(val index: Int? = null) : EditorAction
    data class UpdateBlock(val index: Int, val newText: String) : EditorAction
    data class RemoveBlock(val index: Int) : EditorAction
    data class AddBlocks(val index: Int, val newBlocks: List<String>) : EditorAction
    data class SplitBlock(val index: Int, val cursorStart: Int, val onFocusCalculated: (newFocusIndex: Int) -> Unit) :
        EditorAction

    data class SwapBlocks(val fromIndex: Int, val toIndex: Int) : EditorAction

    data class BlockTurnedIntoMoreBlocks(val index: Int, val newBlocks: List<String>) : EditorAction
    data class EvaluateBlockOnFocusLost(
        val index: Int,
        val currentFocusedIndex: Int?,
        val onFocusAdjusted: (newFocus: Int?) -> Unit
    ) : EditorAction

    data class MergeWithPrevBlock(val index: Int) : EditorAction
    data class PasteImages(
        val imageBytesList: List<ByteArray>,
        val focusedIndex: Int,
        val onFocusCalculated: (newFocusIndex: Int) -> Unit
    ) : EditorAction

    data object SyncNotes : EditorAction
    data class ResolveSingleConflict(val filePath: String, val keepLocalChanges: Boolean) : EditorAction
    data class ResolveAllConflicts(val keepLocalChanges: Boolean) : EditorAction
    data object AbortConflictResolve : EditorAction
}