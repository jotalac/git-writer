package dev.jotalac.core.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Ends the editing of the note when something outside of the editor is pressed - the sidebar, the title bar.
 * The open editor says how in [onOutsidePress]; without one the focus is just cleared.
 */
class EditorUnfocus {
    var onOutsidePress: (() -> Unit)? = null
}

val LocalEditorUnfocus = staticCompositionLocalOf { EditorUnfocus() }

/** A press here takes the cursor out of the note. The press itself is left alone, it still does what it does. */
fun Modifier.unfocusEditorOnPress(): Modifier = composed {
    val editorUnfocus = LocalEditorUnfocus.current
    val focusManager = LocalFocusManager.current
    pointerInput(editorUnfocus) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            editorUnfocus.onOutsidePress?.invoke() ?: focusManager.clearFocus()
        }
    }
}
