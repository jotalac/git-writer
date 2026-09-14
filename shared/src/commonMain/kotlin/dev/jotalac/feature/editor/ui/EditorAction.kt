package dev.jotalac.feature.editor.ui

sealed interface EditorAction {
    data class AddBlock(val index: Int? = null) : EditorAction
    data class UpdateBlock(val index: Int, val newText: String) : EditorAction
    data class RemoveBlock(val index: Int) : EditorAction
    data class AddBlocks(val index: Int, val newBlocks: List<String>) : EditorAction
    data class SetBlocks(val blocks: List<String>) : EditorAction
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

    // tab management
    data object CloseActiveTab : EditorAction
    data object NewTab : EditorAction
    data object NextTab : EditorAction
    data object PreviousTab : EditorAction

    // note management
    data object NewNote : EditorAction
}

/** True for actions that change block content, which is what autosave listens to. */
fun EditorAction.isBlockEditing(): Boolean = when (this) {
    is EditorAction.AddBlock,
    is EditorAction.UpdateBlock,
    is EditorAction.RemoveBlock,
    is EditorAction.AddBlocks,
    is EditorAction.SetBlocks,
    is EditorAction.SplitBlock,
    is EditorAction.SwapBlocks,
    is EditorAction.BlockTurnedIntoMoreBlocks,
    is EditorAction.EvaluateBlockOnFocusLost,
    is EditorAction.MergeWithPrevBlock,
    is EditorAction.PasteImages -> true

    else -> false
}