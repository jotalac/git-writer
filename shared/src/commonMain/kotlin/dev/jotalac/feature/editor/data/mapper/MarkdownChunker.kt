package dev.jotalac.feature.editor.data.mapper

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

fun chunkMarkdownIntoBlocks(fullText: CharSequence): List<String> {
    // normalize the text, because some not normal people use Windows
    val normalizedText: CharSequence = fullText.toString().replace("\r\n", "\n").replace("\r", "\n")

    // initialize the parser with GitHub Flavored Markdown rules
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour, cancellationToken = CancellationToken.NonCancellable)

    // generate the Tree (The Map)
    val parsedTree = parser.buildMarkdownTreeFromString(normalizedText)

    // slice the original text using the Tree's coordinates
    val blocks = mutableListOf<String>()

    var previousContentEnd = -1

    for (range in contentRanges(parsedTree.children, normalizedText)) {
        // make sure the empty lines are inserted (by default line ends with two new-lines)
        if (previousContentEnd >= 0) {
            val emptyBlockCount = emptyBlocksInGap(normalizedText, previousContentEnd, range.first)
            repeat(emptyBlockCount) {
                blocks.add("")
            }
        }

        // Cut the specific chunk out of the original string
        blocks.add(normalizedText.substring(range.first, range.last))
        previousContentEnd = range.last
    }

    return blocks
}

private fun emptyBlocksInGap(text: CharSequence, from: Int, to: Int): Int {
    val newlineCount = text.subSequence(from, to).count { it == '\n' }
    return ((newlineCount - 2) / 2).coerceAtLeast(0)
}

/**
 * Returns [start, end) ranges of the top-level content blocks.
 * Markdown merges list items separated by any number of blank lines into one (loose) list,
 * so lists are split wherever the gap between two items is big enough to hold an empty block.
 */
private fun contentRanges(nodes: List<ASTNode>, text: CharSequence): List<IntRange> {
    val ranges = mutableListOf<IntRange>()

    for (node in nodes) {
        // We only want actual content blocks, not the empty white-space between them
        if (node.type.name == "WHITE_SPACE" || node.type.name == "EOL") continue

        val isList = node.type == MarkdownElementTypes.UNORDERED_LIST || node.type == MarkdownElementTypes.ORDERED_LIST
        if (!isList) {
            ranges.add(node.startOffset..node.endOffset)
            continue
        }

        // for lists
        var segmentStart = node.startOffset
        var previousItemEnd = -1
        for (item in node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }) {
            if (previousItemEnd >= 0 && emptyBlocksInGap(text, previousItemEnd, item.startOffset) > 0) {
                ranges.add(segmentStart..previousItemEnd)
                segmentStart = item.startOffset
            }
            previousItemEnd = trimTrailingWhitespace(text, item.startOffset, item.endOffset)
        }
        ranges.add(segmentStart..node.endOffset)
    }

    return ranges
}

// list items of loose lists can include the trailing blank lines
private fun trimTrailingWhitespace(text: CharSequence, start: Int, end: Int): Int {
    var trimmedEnd = end
    while (trimmedEnd > start && text[trimmedEnd - 1].isWhitespace()) trimmedEnd--
    return trimmedEnd
}
