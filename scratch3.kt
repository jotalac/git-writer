import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

fun main() {
    val fullText = """test
test two
```kotlin
val test = "test"
```"""
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour, cancellationToken = CancellationToken.NonCancellable)
    val parsedTree = parser.buildMarkdownTreeFromString(fullText)
    
    val blocks = mutableListOf<String>()
    for (node in parsedTree.children) {
        if (node.type.name != "WHITE_SPACE" && node.type.name != "EOL") {
            val blockText = fullText.substring(node.startOffset, node.endOffset)
            blocks.add(blockText)
        }
    }
    
    blocks.forEachIndexed { i, block ->
        println("Block $i: $block")
    }
}
