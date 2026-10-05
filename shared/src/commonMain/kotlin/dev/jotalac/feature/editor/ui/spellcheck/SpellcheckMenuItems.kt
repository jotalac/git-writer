package dev.jotalac.feature.editor.ui.spellcheck

/**
 * One entry the editor contributes to the text field's context menu.
 *
 * Modelled explicitly instead of as a disabled action, so the renderer never has to infer the
 * grouping from item positions or counts.
 */
sealed interface SpellcheckMenuItem {

    /** Group label. Rendered dimmed and not clickable. */
    data class Header(val label: String) : SpellcheckMenuItem

    /** Clickable entry. */
    data class Action(val label: String, val onClick: () -> Unit) : SpellcheckMenuItem

    /** Visual separator between groups. */
    data object Divider : SpellcheckMenuItem
}

/**
 * Builds the spelling block appended to the field's context menu: a separator, a header, the
 * suggestions for the clicked word, then the entry that persists that word.
 *
 * Pure, so the menu structure can be tested without a running spell engine.
 */
fun buildSpellcheckMenuItems(
    menu: SpellcheckMenu,
    suggestionsHeader: String,
    onReplaceWith: (menu: SpellcheckMenu, suggestion: String) -> Unit,
    onAddToDictionary: (menu: SpellcheckMenu) -> Unit,
): List<SpellcheckMenuItem> = buildList {
    add(SpellcheckMenuItem.Divider)
    add(SpellcheckMenuItem.Header(suggestionsHeader))

    menu.suggestions.forEach { suggestion ->
        add(SpellcheckMenuItem.Action(suggestion) { onReplaceWith(menu, suggestion) })
    }

    add(SpellcheckMenuItem.Divider)
    add(SpellcheckMenuItem.Action(menu.addToDictionaryLabel) { onAddToDictionary(menu) })
}

/**
 * Drops the spans whose word is already in [dictionaryWords], so a word the user just added stops
 * being underlined at once instead of waiting for the next check.
 */
fun List<MisspelledSpan>.excludingDictionaryWords(
    text: String,
    dictionaryWords: Set<String>,
): List<MisspelledSpan> = filter { span ->
    span.end <= text.length && text.substring(span.start, span.end) !in dictionaryWords
}

/**
 * The text offset the spelling menu should look up for a caret at [caret].
 *
 * A caret sits *after* the character it follows and a word range excludes its end, so a caret at the
 * end of a word moves back one character. Typing a word and pressing the shortcut then finds the
 * word just typed, rather than nothing or the next word.
 */
fun spellcheckAnchorForCaret(text: String, caret: Int): Int {
    val at = caret.coerceIn(0, text.length)
    val afterWord = at > 0 && text[at - 1].isLetterOrDigit()
    val insideWord = at < text.length && text[at].isLetterOrDigit()

    return if (afterWord && !insideWord) at - 1 else at
}
