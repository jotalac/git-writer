package dev.jotalac.feature.editor.data.mapperw

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

fun chunkMarkdownIntoBlocks(fullText: CharSequence): List<String> {
    // 1. Initialize the parser with GitHub Flavored Markdown rules
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour, cancellationToken = CancellationToken.NonCancellable)

    // 2. Generate the Tree (The Map)
    val parsedTree = parser.buildMarkdownTreeFromString(fullText)

    // 3. Slice the original text using the Tree's coordinates
    val blocks = mutableListOf<String>()

    for (node in parsedTree.children) {
        // We only want actual content blocks, not the empty white-space between them
        if (node.type.name != "WHITE_SPACE" && node.type.name != "EOL") {
            // Cut the specific chunk out of the original string
            val blockText = fullText.substring(node.startOffset, node.endOffset)
            blocks.add(blockText)
        }
    }

    return blocks
}
