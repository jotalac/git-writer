package dev.jotalac.feature.editor.ui.spellcheck

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import dev.nucleusframework.spellcheck.SpellChecker
import dev.nucleusframework.spellcheck.buildSpellcheckMenuModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * How long typing has to pause before the text is checked. Checking on every keystroke
 * would call into the native engine once per character.
 */
private val SPELLCHECK_DEBOUNCE = 300.milliseconds

/** Spans kept together with the exact text they were computed from. */
private data class CheckedText(val text: String, val spans: List<MisspelledSpan>)

/**
 * Desktop spell check through Nucleus, which wraps the engine the OS already ships:
 * NSSpellChecker on macOS, the Spell Checking API on Windows, Hunspell on Linux.
 *
 * When no engine or dictionary is installed every Nucleus call is a documented no-op
 * (`misspellings` returns an empty list), so a machine without Hunspell simply shows no
 * squiggles instead of failing.
 */
@Composable
actual fun rememberMisspelledSpans(text: String): List<MisspelledSpan> {
    // Loading a dictionary touches the disk, so warm the session once, off the UI thread.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { runCatching { SpellChecker.ensureSession() } }
    }

    var checked by remember { mutableStateOf<CheckedText?>(null) }

    LaunchedEffect(text) {
        delay(SPELLCHECK_DEBOUNCE)
        val spans = withContext(Dispatchers.Default) {
            if (!SpellChecker.isAvailable) {
                emptyList()
            } else {
                SpellChecker.misspellings(text).map { MisspelledSpan(it.start, it.end) }
            }
        }
        checked = CheckedText(text, spans)
    }

    // Never hand back spans belonging to older text: their offsets may no longer exist.
    return checked?.takeIf { it.text == text }?.spans.orEmpty()
}

/**
 * Runs the native check for one word. Uses [SpellChecker.sessionIfReady] so a right-click
 * never blocks the UI thread while a dictionary loads; without a warmed session the menu
 * simply stays empty until the next click.
 */
actual fun spellcheckMenuFor(text: String, anchor: Int?): SpellcheckMenu? {
    if (anchor == null) return null
    val session = SpellChecker.sessionIfReady ?: return null
    val model = buildSpellcheckMenuModel(text, anchor, session) ?: return null

    return SpellcheckMenu(
        word = model.word,
        start = model.range?.start ?: -1,
        end = model.range?.end ?: -1,
        suggestions = model.suggestions,
        addToDictionaryLabel = model.addToDictionaryLabel,
    )
}

actual fun addWordToDictionary(word: String) {
    SpellChecker.addToDictionary(word)
}

/**
 * Merges the suggestions into the text field's context menu, rendered with the app's own
 * chrome by [AppContextMenuRepresentation] rather than Compose's bundled representations.
 */
@Composable
actual fun SpellcheckMenuHost(
    items: () -> List<SpellcheckAction>,
    content: @Composable () -> Unit,
) {
    val latestItems = rememberUpdatedState(items)
    val representation = remember {
        AppContextMenuRepresentation(spellcheckItemCount = { latestItems.value().size })
    }

    CompositionLocalProvider(LocalContextMenuRepresentation provides representation) {
        ContextMenuDataProvider(
            items = {
                items().map { action ->
                    ContextMenuItem(
                        label = action.label,
                        enabled = action.enabled,
                        onClick = action.onClick,
                    )
                }
            },
            content = content,
        )
    }
}
