package dev.jotalac.feature.editor.ui

sealed interface EditorAction {
    /** The note's whole text after an edit. */
    data class UpdateText(val text: String) : EditorAction

    /** Saves the images into the notebook, then hands their markdown to [onImagesSaved] to put into the note. */
    data class PasteImages(
        val imageBytesList: List<ByteArray>,
        val onImagesSaved: (imageMarkdown: List<String>) -> Unit,
    ) : EditorAction

    data object SyncNotes : EditorAction
    // e.g. the app came back to the foreground - the network may have changed meanwhile
    data object CheckRemoteStatus : EditorAction
    // the app goes to the background - a last sync with auto sync on
    data object SyncBeforeLeaving : EditorAction
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

/** True for actions that change the note's content, which is what autosave listens to. */
fun EditorAction.isNoteEditing(): Boolean = this is EditorAction.UpdateText
