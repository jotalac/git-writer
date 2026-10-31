package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import dev.jotalac.core.utils.hasClipboardImage
import dev.jotalac.core.utils.isMacOsPlatform
import dev.jotalac.feature.editor.ui.MarkdownEditorState

/** The context menu shortcut: Shift+F10 on Windows/Linux, Control+Return on macOS. */
internal fun isContextMenuShortcut(event: KeyEvent): Boolean =
    if (isMacOsPlatform) {
        event.isCtrlPressed && event.key == Key.Enter
    } else {
        event.isShiftPressed && event.key == Key.F10
    }

/** The keys of a key down [event] the editor handles itself - formatting shortcuts, indentation, moving lines... */
internal fun handleEditorKey(event: KeyEvent, editorState: MarkdownEditorState, pasteClipboardImage: () -> Unit): Boolean {
    val isShortcut = event.isCtrlPressed || event.isMetaPressed

    fun handled(action: () -> Unit): Boolean {
        action()
        return true
    }

    return when (event.key) {
        Key.Escape -> handled { editorState.clearFocus() }
        // Enter itself goes to the text field - its line break continues a list, see MarkdownEditorState
        Key.Enter, Key.NumPadEnter -> event.isShiftPressed && handled { editorState.insertPlainNewLine() }
        Key.Tab -> !isShortcut && handled { editorState.indent(isUntab = event.isShiftPressed) }
        Key.DirectionUp -> event.isAltPressed && handled { editorState.moveLinesUp() }
        Key.DirectionDown -> event.isAltPressed && handled { editorState.moveLinesDown() }
        Key.V -> isShortcut && hasClipboardImage() && handled(pasteClipboardImage)
        Key.B -> isShortcut && handled { editorState.applyBold() }
        Key.I -> isShortcut && handled { editorState.applyItalic() }
        Key.K -> isShortcut && handled { editorState.addLinkTemplate() }
        Key.E -> isShortcut && handled { editorState.applyInlineCode() }
        else -> false
    }
}
