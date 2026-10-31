package dev.jotalac.core.utils

import androidx.compose.ui.focus.FocusRequester

/** Focuses this requester's node - nothing happens while it is not in the composition, where [FocusRequester.requestFocus] throws. */
fun FocusRequester.requestFocusSafely() {
    try {
        requestFocus()
    } catch (_: IllegalStateException) {
    }
}
