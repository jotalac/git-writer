package dev.jotalac.feature.editor.ui.spellcheck

import kotlin.test.Test
import kotlin.test.assertEquals

class SpellcheckMenuItemsTest {

    private val menu = SpellcheckMenu(
        word = "tohle",
        start = 4,
        end = 9,
        suggestions = listOf("thole", "toile"),
        addToDictionaryLabel = "Add to dictionary",
    )

    /** Structure of the list without the click lambdas, which are not comparable. */
    private fun List<SpellcheckMenuItem>.describe(): List<String> = map { item ->
        when (item) {
            is SpellcheckMenuItem.Header -> "header:${item.label}"
            is SpellcheckMenuItem.Action -> "action:${item.label}"
            SpellcheckMenuItem.Divider -> "divider"
        }
    }

    @Test
    fun `suggestions sit between dividers with the header above them`() {
        val items = buildSpellcheckMenuItems(
            menu = menu,
            suggestionsHeader = "Suggestions",
            onReplaceWith = { _, _ -> },
            onAddToDictionary = {},
        )

        assertEquals(
            listOf(
                "divider",
                "header:Suggestions",
                "action:thole",
                "action:toile",
                "divider",
                "action:Add to dictionary",
            ),
            items.describe(),
        )
    }

    @Test
    fun `a word with no suggestions still offers the dictionary entry`() {
        val items = buildSpellcheckMenuItems(
            menu = menu.copy(suggestions = emptyList()),
            suggestionsHeader = "Suggestions",
            onReplaceWith = { _, _ -> },
            onAddToDictionary = {},
        )

        assertEquals(
            listOf("divider", "header:Suggestions", "divider", "action:Add to dictionary"),
            items.describe(),
        )
    }

    @Test
    fun `choosing a suggestion reports it together with the menu it came from`() {
        var chosen: Pair<String, SpellcheckMenu>? = null
        val items = buildSpellcheckMenuItems(
            menu = menu,
            suggestionsHeader = "Suggestions",
            onReplaceWith = { source, suggestion -> chosen = suggestion to source },
            onAddToDictionary = {},
        )

        val suggestionItem = items.filterIsInstance<SpellcheckMenuItem.Action>()
            .first { it.label == "toile" }
        suggestionItem.onClick()

        assertEquals("toile" to menu, chosen)
    }

    @Test
    fun `choosing the dictionary entry reports the menu`() {
        var chosen: SpellcheckMenu? = null
        val items = buildSpellcheckMenuItems(
            menu = menu,
            suggestionsHeader = "Suggestions",
            onReplaceWith = { _, _ -> },
            onAddToDictionary = { source -> chosen = source },
        )

        val dictionaryItem = items.filterIsInstance<SpellcheckMenuItem.Action>()
            .first { it.label == menu.addToDictionaryLabel }
        dictionaryItem.onClick()

        assertEquals(menu, chosen)
    }

    @Test
    fun `spans of words already in the dictionary are dropped`() {
        val text = "helo wrld"
        val spans = listOf(MisspelledSpan(0, 4), MisspelledSpan(5, 9))

        assertEquals(
            listOf(MisspelledSpan(5, 9)),
            spans.excludingDictionaryWords(text, setOf("helo")),
        )
    }

    @Test
    fun `spans that no longer fit the text are dropped`() {
        assertEquals(
            emptyList(),
            listOf(MisspelledSpan(0, 20)).excludingDictionaryWords("helo", emptySet()),
        )
    }

    @Test
    fun `replacing uses the engine range when there is one`() {
        // in "a tohle b" the engine reported [2, 7), which is exactly the misspelled word
        val ranged = SpellcheckMenu("tohle", 2, 7, emptyList(), "x")

        assertEquals("a thole b", ranged.replacingWordIn("a tohle b", "thole"))
    }

    @Test
    fun `replacing falls back to the first occurrence when the range is unknown`() {
        val withoutRange = SpellcheckMenu("tohle", -1, -1, emptyList(), "x")

        assertEquals("thole and tohle", withoutRange.replacingWordIn("tohle and tohle", "thole"))
    }

    @Test
    fun `the shortcut anchor lands on the word just typed`() {
        // a caret sits after the word, and a word range excludes its end
        assertEquals(3, spellcheckAnchorForCaret("helo", 4))
        assertEquals(3, spellcheckAnchorForCaret("helo ", 4))

        // inside a word, and in whitespace, the caret is already the right anchor
        assertEquals(6, spellcheckAnchorForCaret("helo wrld", 6))
        assertEquals(5, spellcheckAnchorForCaret("helo wrld", 5))

        assertEquals(0, spellcheckAnchorForCaret("", 0))
    }
}
