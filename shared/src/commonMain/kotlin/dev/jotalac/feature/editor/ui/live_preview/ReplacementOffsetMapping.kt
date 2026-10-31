package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.text.input.OffsetMapping

/**
 * Maps the offsets between the source and the shown text, where every replacement (sorted, not overlapping) shows
 * its own text in place of its source range.
 */
internal class ReplacementOffsetMapping(
    private val sourceLength: Int,
    private val replacements: List<ReplacingDecoration>,
) : OffsetMapping {
    // where each replacement starts in the shown text
    private val shownStarts = IntArray(replacements.size)
    private val shownLength: Int

    init {
        var shift = 0
        replacements.forEachIndexed { index, replacement ->
            shownStarts[index] = replacement.start + shift
            shift += replacement.replacement.length - (replacement.end - replacement.start)
        }
        shownLength = sourceLength + shift
    }

    override fun originalToTransformed(offset: Int): Int {
        val index = lastIndexWhere { replacements[it].start <= offset }
        if (index < 0) return offset.coerceIn(0, shownLength)

        val replacement = replacements[index]
        val shownStart = shownStarts[index]
        val shownEnd = shownStart + replacement.replacement.length
        val result = when {
            offset >= replacement.end -> shownEnd + (offset - replacement.end)
            else -> shownStart + minOf(offset - replacement.start, replacement.replacement.length)
        }
        return result.coerceIn(0, shownLength)
    }

    override fun transformedToOriginal(offset: Int): Int {
        // the last one starting there - hidden syntax next to each other all starts at the same shown offset
        val index = lastIndexWhere { shownStarts[it] <= offset }
        if (index < 0) return offset.coerceIn(0, sourceLength)

        val replacement = replacements[index]
        val shownStart = shownStarts[index]
        val shownEnd = shownStart + replacement.replacement.length
        val result = when {
            // a click on a widget's line goes to its start
            replacement is Widget && offset < shownEnd -> replacement.start
            offset >= shownEnd -> replacement.end + (offset - shownEnd)
            else -> replacement.start + minOf(offset - shownStart, replacement.end - replacement.start)
        }
        return result.coerceIn(0, sourceLength)
    }

    // binary search for the last replacement matching [predicate], which holds for a prefix of them
    private inline fun lastIndexWhere(predicate: (Int) -> Boolean): Int {
        var low = 0
        var high = replacements.size - 1
        var found = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (predicate(middle)) {
                found = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return found
    }
}
