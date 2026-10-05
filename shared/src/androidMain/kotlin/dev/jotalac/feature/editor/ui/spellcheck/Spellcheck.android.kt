package dev.jotalac.feature.editor.ui.spellcheck

import androidx.compose.runtime.Composable

/**
 * Android spell-checks through the system keyboard, which already underlines and corrects
 * misspellings, so the editor does not add its own squiggles here.
 */
@Composable
actual fun rememberMisspelledSpans(text: String): List<MisspelledSpan> = emptyList()

actual fun spellcheckMenuFor(text: String, anchor: Int?): SpellcheckMenu? = null

actual suspend fun addWordToDictionary(word: String) {}

/** Nothing to merge into the context menu; the field keeps its own items. */
@Composable
actual fun SpellcheckMenuHost(
    items: () -> List<SpellcheckMenuItem>,
    controller: SpellcheckMenuController,
    content: @Composable () -> Unit,
) = content()
