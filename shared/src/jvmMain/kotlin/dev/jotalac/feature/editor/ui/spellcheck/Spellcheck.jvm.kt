package dev.jotalac.feature.editor.ui.spellcheck

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.nucleusframework.spellcheck.SpellChecker
import dev.nucleusframework.spellcheck.buildSpellcheckMenuModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private val SPELLCHECK_DEBOUNCE = 300.milliseconds

/** misspelled spans kept together with the exact text they were computed from. */
private data class CheckedText(val text: String, val spans: List<MisspelledSpan>)

/**
 * init the spellchecker and load the current language
 */
private fun loadSessionForCurrentLanguage(): Boolean = runCatching {
    SpellChecker.locale = Locale.getDefault()
    SpellChecker.ensureSession().isAvailable
}.getOrDefault(false)

/**
 * On every text change (with debounce) check the misspelled spans with the OS native spell checker
 */
@Composable
actual fun rememberMisspelledSpans(text: String): List<MisspelledSpan> {
    var checked by remember { mutableStateOf<CheckedText?>(null) }

    LaunchedEffect(text) {
        delay(SPELLCHECK_DEBOUNCE)
        val spans = withContext(Dispatchers.Default) {
            if (!loadSessionForCurrentLanguage()) {
                emptyList()
            } else {
                SpellChecker.misspellings(text).map {  MisspelledSpan(it.start, it.end) }
            }
        }
        checked = CheckedText(text, spans)
    }

    // never hand back spans belonging to older text: their offsets may no longer exist.
    return checked?.takeIf { it.text == text }?.spans.orEmpty()
}

/**
 * does the native OS spell check for one word
 * make sure the session is ready, else returns null
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

actual suspend fun addWordToDictionary(word: String) {
    withContext(Dispatchers.IO) { SpellChecker.addToDictionary(word) }
}

/**
 * Merges the suggestions into the text field's context menu
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun SpellcheckMenuHost(
    items: () -> List<SpellcheckMenuItem>,
    controller: SpellcheckMenuController,
    content: @Composable () -> Unit,
) {
    // read before providing, so the wrapper delegates to whatever was already installed
    val delegate = LocalTextContextMenu.current
    val textContextMenu = remember(delegate, controller) {
        SpellcheckTextContextMenu(delegate, controller)
    }
    val representation = remember { AppContextMenuRepresentation() }

    CompositionLocalProvider(
        LocalTextContextMenu provides textContextMenu,
        LocalContextMenuRepresentation provides representation,
    ) {
        ContextMenuDataProvider(
            items = { items().map(::toContextMenuItem) },
            content = content,
        )
    }
}

/**
 * Compose builds the text field's context menu state internally and never exposes it. Delegating
 * through this wrapper captures the state the field hands to [TextContextMenu.Area] and passes it to
 * [controller], without changing how the menu is built or when a secondary click opens it.
 */
@OptIn(ExperimentalFoundationApi::class)
private class SpellcheckTextContextMenu(
    private val delegate: TextContextMenu,
    private val controller: SpellcheckMenuController,
) : TextContextMenu {
    @Composable
    override fun Area(
        textManager: TextContextMenu.TextManager,
        state: ContextMenuState,
        content: @Composable () -> Unit,
    ) {
        // the field composes its menu area below the editor that opens it, and a CompositionLocal
        // cannot be read upwards, so the controller is filled in from here
        SideEffect {
            controller.openAt = { anchor ->
                state.status = ContextMenuState.Status.Open(anchor)
            }
        }

        delegate.Area(textManager, state, content)
    }
}

/**
 * Converts the spellcheck menu item to the context menu item, so that it can be processed by the compose context menu
 */
private fun toContextMenuItem(item: SpellcheckMenuItem): ContextMenuItem = when (item) {
    is SpellcheckMenuItem.Header -> ContextMenuItem(item.label, enabled = false, onClick = {})
    is SpellcheckMenuItem.Action -> ContextMenuItem(item.label, enabled = true, onClick = item.onClick)
    SpellcheckMenuItem.Divider -> SeparatorMenuItem()
}
