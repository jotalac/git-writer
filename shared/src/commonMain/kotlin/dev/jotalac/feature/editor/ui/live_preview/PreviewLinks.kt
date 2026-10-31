package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult

/**
 * The link at [position] that a click opens: one shown as a link (its syntax hidden), or any with Ctrl/Cmd held -
 * a click on a link that is being edited only puts the cursor in it.
 */
internal fun LivePreviewText.linkAt(
    layout: TextLayoutResult,
    position: Offset,
    keyboardModifiers: PointerKeyboardModifiers,
): LinkTarget? {
    val offset = layout.getOffsetForPosition(position)
    // the nearest character - the position has to be on it, not somewhere beside the line
    val character = listOf(offset, offset - 1).firstOrNull {
        it in 0 until layout.layoutInput.text.length && layout.getBoundingBox(it).contains(position)
    } ?: return null
    val withModifier = keyboardModifiers.isCtrlPressed || keyboardModifiers.isMetaPressed
    return links.firstOrNull { character >= it.start && character < it.end }
        ?.decoration
        ?.takeIf { !it.isEdited || withModifier }
}

/**
 * Watches the pointer before the text field gets it (the Initial pass), which still gets every event: tells whether
 * a button is down, and whether the pointer is over a link ([linkAt]). A click on a link opens it with [onLinkClick]
 * instead of putting the cursor there.
 */
internal fun Modifier.previewPointerInput(
    linkAt: (Offset, PointerKeyboardModifiers) -> LinkTarget?,
    onPointerDownChange: (Boolean) -> Unit,
    onOverLinkChange: (Boolean) -> Unit,
    onLinkClick: (LinkTarget) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            onPointerDownChange(event.changes.any { it.pressed })
            val change = event.changes.firstOrNull() ?: continue

            val link = linkAt(change.position, event.keyboardModifiers)
            onOverLinkChange(link != null)
            if (event.type != PointerEventType.Press || link == null) continue

            change.consume()
            val release = waitForUpOrCancellation(PointerEventPass.Initial)
            onPointerDownChange(false)
            if (release != null) {
                release.consume()
                onLinkClick(link)
            }
        }
    }
}
