package dev.jotalac.feature.editor.data.mapper

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

    for (node in parsedTree.children) {
        // We only want actual content blocks, not the empty white-space between them
        if (node.type.name == "WHITE_SPACE" || node.type.name == "EOL") continue

        // make sure the empty lines are inserted (by default line ends with two new-lines)
        if (previousContentEnd >= 0) {
            val gap = normalizedText.substring(previousContentEnd, node.startOffset)
            val newlineCount = gap.count { it == '\n' }
            val emptyBlockCount = ((newlineCount - 2) / 2).coerceAtLeast(0)
            repeat(emptyBlockCount) {
                blocks.add("")
            }
        }

        // Cut the specific chunk out of the original string
        blocks.add(normalizedText.substring(node.startOffset, node.endOffset))
        previousContentEnd = node.endOffset
    }

    return blocks
}
