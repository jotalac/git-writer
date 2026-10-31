package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.OffsetMapping
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import dev.jotalac.feature.editor.ui.spellcheck.LocalSpellcheckEnabled
import dev.jotalac.feature.editor.ui.spellcheck.LocalSpellcheckLanguage
import dev.jotalac.feature.editor.ui.spellcheck.MisspelledSpan
import dev.jotalac.feature.editor.ui.spellcheck.SpellcheckMenuController
import dev.jotalac.feature.editor.ui.spellcheck.SpellcheckMenuItem
import dev.jotalac.feature.editor.ui.spellcheck.addWordToDictionary
import dev.jotalac.feature.editor.ui.spellcheck.buildSpellcheckMenuItems
import dev.jotalac.feature.editor.ui.spellcheck.excludingDictionaryWords
import dev.jotalac.feature.editor.ui.spellcheck.rememberMisspelledSpans
import dev.jotalac.feature.editor.ui.spellcheck.spellcheckAnchorForCaret
import dev.jotalac.feature.editor.ui.spellcheck.spellcheckMenuFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The spell check of the note (desktop - on mobile the keyboard checks the spelling): its menu of suggestions. */
@Stable
internal class PreviewSpellcheck(private val editorState: MarkdownEditorState, private val scope: CoroutineScope) {
    val menuController = SpellcheckMenuController()

    // The offset in the note the menu is asked about: the clicked word, or the caret for the shortcut.
    var menuAnchor by mutableStateOf<Int?>(null)

    // words added to the dictionary from the menu, hidden at once instead of waiting for the next check
    var dictionaryWords by mutableStateOf(emptySet<String>())
        private set

    // Read by the platform's menu when it opens.
    fun menuItems(suggestionsHeader: String): List<SpellcheckMenuItem> =
        spellcheckMenuFor(editorState.value.text, menuAnchor)?.let { menu ->
            buildSpellcheckMenuItems(
                menu = menu,
                suggestionsHeader = suggestionsHeader,
                onReplaceWith = { target, suggestion -> editorState.replaceRange(target.start, target.end, suggestion) },
                onAddToDictionary = { target ->
                    dictionaryWords = dictionaryWords + target.word
                    scope.launch { addWordToDictionary(target.word) }
                },
            )
        }.orEmpty()

    /** The context menu shortcut - opens at the caret the menu a secondary click opens at the pointer. */
    fun openMenuAtCaret(layout: TextLayoutResult?, mapping: OffsetMapping) {
        val caret = editorState.value.selection.start
        // the menu needs the word under the caret, or it opens without any suggestions
        menuAnchor = spellcheckAnchorForCaret(editorState.value.text, caret)
        layout?.let { menuController.showAt(it.getCursorRect(mapping.originalToTransformed(caret))) }
    }
}

@Composable
internal fun rememberPreviewSpellcheck(editorState: MarkdownEditorState): PreviewSpellcheck {
    val scope = rememberCoroutineScope()
    return remember(editorState, scope) { PreviewSpellcheck(editorState, scope) }
}

/** The misspelled words of [text] to underline, in the shown text - code, links and maths are not words to check. */
@Composable
internal fun PreviewSpellcheck.underlines(
    text: String,
    decorations: List<PreviewDecoration>,
    mapping: OffsetMapping,
): List<MisspelledSpan> {
    if (!LocalSpellcheckEnabled.current) return emptyList()
    // null when the spell check language follows the app language
    val misspelled = rememberMisspelledSpans(text, LocalSpellcheckLanguage.current)
    val unchecked = remember(decorations) { decorations.filter { it.isUnchecked() } }

    return misspelled.excludingDictionaryWords(text, dictionaryWords)
        .filter { span -> unchecked.none { span.start < it.end && span.end > it.start } }
        .mapNotNull { span ->
            // the hidden syntax moves the words
            val start = mapping.originalToTransformed(span.start)
            val end = mapping.originalToTransformed(span.end)
            if (end > start) MisspelledSpan(start, end) else null
        }
}

private val UNCHECKED_LOOKS = setOf(TextLook.Code, TextLook.Monospace, TextLook.Math, TextLook.Link, TextLook.Syntax)

private fun PreviewDecoration.isUnchecked(): Boolean =
    this is CodeBackground || this is Widget || this is InlineMath || this is Look && look in UNCHECKED_LOOKS
