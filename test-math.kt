import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

fun main() {
    val text = "$$\n\\frac{d}{dx}(x^2) = 2x\n$$"
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour)
    val tree = parser.buildMarkdownTreeFromString(text)
    println(tree.children.map { it.type.name })
}
