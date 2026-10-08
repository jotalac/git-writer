package dev.jotalac.feature.editor.ui.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Word that was rejected by the platform spell checker (start and end indices).
 */
data class MisspelledSpan(val start: Int, val end: Int)

/**
 * Language the editor checks spelling in, as a language tag, or null to follow the app
 * language. Provided at the app root from the settings.
 */
val LocalSpellcheckLanguage = staticCompositionLocalOf<String?> { null }

/**
 * Misspelled spans of [text], or an empty list when the platform has no spell checker.
 *
 * @param language the spell check language, or null to follow the app language.
 */
@Composable
expect fun rememberMisspelledSpans(text: String, language: String?): List<MisspelledSpan>

/**
 * Suggestions for the misspelled word that contains [anchor] in [text], or `null` when the
 * anchor is missing, the word is spelled correctly, or there is no engine.
 */
expect fun spellcheckMenuFor(text: String, anchor: Int?): SpellcheckMenu?

/** Persists [word] in the user dictionary so the engine stops flagging it. No-op without an engine. */
expect suspend fun addWordToDictionary(word: String)

/**
 * Wraps the editor field so the platform can render spelling [items] together with the field's own
 * commands. Desktop shows one menu made of both; mobile ignores [items] and renders [content] as is.
 * [controller] is filled in by the platform, which is the only place the field's menu state exists.
 */
@Composable
expect fun SpellcheckMenuHost(
    items: () -> List<SpellcheckMenuItem>,
    controller: SpellcheckMenuController,
    content: @Composable () -> Unit,
)

/**
 * opens the context menu for item
 * needed to make it work, that the context menu is opened with keyboard shortcut
 *
 * Compose creates the field's menu state below the editor that opens it, and a CompositionLocal
 * cannot be read upwards, so the platform menu area fills [openAt] in and the editor calls
 * [showAt] later.
 */
@Stable
class SpellcheckMenuController {

    internal var openAt: (anchor: Rect) -> Unit = {}

    /** No-op where the platform has no spelling menu. */
    fun showAt(anchor: Rect) = openAt(anchor)
}

/** A misspelled word and what the menu can do about it. */
data class SpellcheckMenu(
    val word: String,
    val start: Int,
    val end: Int,
    val suggestions: List<String>,
    val addToDictionaryLabel: String,
) {
    /**
     * replace [text] with [suggestion] in place of the misspelled word.
     *
     * Falls back to replacing the first occurrence of [word] when the engine could not
     * report a range, so a menu built from a stale model still does something sensible.
     */
    fun replacingWordIn(text: String, suggestion: String): String =
        if (start in 0..end && end <= text.length) {
            text.replaceRange(start, end, suggestion)
        } else {
            text.replaceFirst(word, suggestion)
        }
}

/**
 * Reports where a secondary click landed inside the field, in the field's text coordinates.
 *
 * Reads the [PointerEventPass.Initial] pass because the text field consumes the press in
 * the main pass to open its own menu; without the early pass this never fires. The event is
 * deliberately left unconsumed so the field still opens that menu.
 */
fun Modifier.onSpellingMenuRequest(onRequest: (Offset) -> Unit): Modifier = composed {
    val latestRequest = rememberUpdatedState(onRequest)
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.buttons.isSecondaryPressed) {
                    event.changes.firstOrNull()?.let { latestRequest.value(it.position) }
                }
            }
        }
    }
}
