package dev.jotalac
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

fun main(args: Array<String>) {
    val text = "$$\n\\frac{d}{dx}(x^2) = 2x\n$$"
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour)
    val tree = parser.buildMarkdownTreeFromString(text)
    println("AST ROOT CHILDREN:")
    tree.children.forEach { println(it.type.name) }
}
