package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.text.TextRange
import dev.jotalac.feature.editor.ui.components.markdown_rendering.latexSource
import dev.jotalac.feature.editor.ui.components.markdown_rendering.textChildren
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

internal fun parseNoteMarkdown(text: CharSequence): ASTNode =
    MarkdownParser(GFMFlavourDescriptor(), cancellationToken = CancellationToken.NonCancellable)
        .buildMarkdownTreeFromString(text)

/**
 * How the text looks like when the cursor is on it - text with visible markdown syntax
 */
internal fun previewDecorations(text: String, root: ASTNode, selection: TextRange?): List<PreviewDecoration> =
    DecorationCollector(text, selection).collect(root)

private val FORMATTING_DELIMITERS = setOf<IElementType>(MarkdownTokenTypes.EMPH, GFMTokenTypes.TILDE, GFMTokenTypes.EQUALS)

// inside code everything is code - `**` there is not bold
private val CODE_DELIMITERS = setOf<IElementType>(MarkdownTokenTypes.BACKTICK)

private val HEADINGS = mapOf(
    MarkdownElementTypes.ATX_1 to 1,
    MarkdownElementTypes.ATX_2 to 2,
    MarkdownElementTypes.ATX_3 to 3,
    MarkdownElementTypes.ATX_4 to 4,
    MarkdownElementTypes.ATX_5 to 5,
    MarkdownElementTypes.ATX_6 to 6,
)

// i dont know if anybody actually uses this settext markdown syntax????
private val SETEXT_HEADINGS = mapOf(MarkdownElementTypes.SETEXT_1 to 1, MarkdownElementTypes.SETEXT_2 to 2)

private val SETEXT_UNDERLINES = setOf<IElementType>(MarkdownTokenTypes.SETEXT_1, MarkdownTokenTypes.SETEXT_2)

private val LIST_MARKERS = setOf<IElementType>(MarkdownTokenTypes.LIST_BULLET, MarkdownTokenTypes.LIST_NUMBER)

private val FENCE_SYNTAX = setOf<IElementType>(
    MarkdownTokenTypes.CODE_FENCE_START,
    MarkdownTokenTypes.FENCE_LANG,
    MarkdownTokenTypes.CODE_FENCE_END,
)

private class DecorationCollector(private val text: String, private val selection: TextRange?) {
    private val decorations = mutableListOf<PreviewDecoration>()

    // how many hidden `>` each line starts with, by the line's start
    private val quoteDepths = mutableMapOf<Int, Int>()

    fun collect(root: ASTNode): List<PreviewDecoration> {
        root.children.forEach { block(it, isTopLevel = true, listDepth = 0) }
        for ((lineStart, depth) in quoteDepths) add(QuoteLine(lineStart, text.lineEndAt(lineStart), depth))
        return decorations
    }

    // the cursor is in the range or right at its edge - its syntax is shown there, so it can be edited
    private fun isEdited(start: Int, end: Int): Boolean =
        selection != null && selection.min <= end && selection.max >= start

    private fun isEdited(node: ASTNode): Boolean = isEdited(node.startOffset, node.endOffset)

    private fun add(decoration: PreviewDecoration) {
        if (decoration.end > decoration.start) decorations += decoration
    }

    private fun look(node: ASTNode, look: TextLook) = add(Look(node.startOffset, node.endOffset, look))

    /** Syntax of [owner] - hidden, or only dimmed while [owner] is being edited. */
    private fun syntax(start: Int, end: Int, owner: ASTNode) {
        add(if (isEdited(owner)) Look(start, end, TextLook.Syntax) else Replacement(start, end, ""))
    }

    private fun firstNonSpace(start: Int, end: Int): Int {
        var index = start
        while (index < end && text[index].isWhitespace()) index++
        return index
    }

    // --- blocks ---

    private fun block(node: ASTNode, isTopLevel: Boolean, listDepth: Int) {
        // if a block is heading
        HEADINGS[node.type]?.let { return atxHeading(node, it) }
        SETEXT_HEADINGS[node.type]?.let { return setextHeading(node, it) }

        // get the block type and render it
        when (node.type) {
            MarkdownElementTypes.PARAGRAPH -> {
                val widget = if (isTopLevel) paragraphWidget(node) else null
                if (widget != null && !isEdited(node)) addWidget(node, widget) else inline(node.children)
            }

            MarkdownElementTypes.CODE_FENCE -> codeFence(node)

            MarkdownElementTypes.CODE_BLOCK -> {
                add(CodeBackground(node.startOffset, node.endOffset))
                look(node, TextLook.Monospace)
            }

            GFMElementTypes.TABLE ->
                if (isTopLevel && !isEdited(node)) addWidget(node, WidgetKind.Table) else look(node, TextLook.Monospace)

            MarkdownTokenTypes.HORIZONTAL_RULE ->
                if (isTopLevel && !isEdited(node)) addWidget(node, WidgetKind.Rule) else look(node, TextLook.Syntax)

            MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> list(node, listDepth)

            // a GitHub alert (> [!TIP]) is a quote with a title
            MarkdownElementTypes.BLOCK_QUOTE, GFMElementTypes.ALERT -> quote(node, listDepth)

            MarkdownElementTypes.LINK_DEFINITION, MarkdownElementTypes.HTML_BLOCK -> look(node, TextLook.Syntax)
        }
    }

    // a paragraph with nothing but an image or a maths expression - drawn like the rendered notes draw it
    private fun paragraphWidget(node: ASTNode): WidgetKind? = when (node.textChildren(text).singleOrNull()?.type) {
        MarkdownElementTypes.IMAGE -> WidgetKind.Image
        GFMElementTypes.BLOCK_MATH, GFMElementTypes.INLINE_MATH -> WidgetKind.Math
        else -> null
    }

    // whole lines - the text keeps a line of its own for the composable
    private fun addWidget(node: ASTNode, kind: WidgetKind) {
        add(Widget(text.lineStartAt(node.startOffset), text.lineEndAt(node.endOffset), kind))
    }

    // the code stays text that can be edited, the fence lines around it are hidden while the cursor is away
    private fun codeFence(node: ASTNode) {
        look(node, TextLook.Monospace)

        // the code between the fence lines - none in a fence that is still being written
        val codeStart = text.indexOf('\n', node.startOffset) + 1
        val closing = node.children.lastOrNull { it.type == MarkdownTokenTypes.CODE_FENCE_END }
        val codeEnd = closing?.let { it.startOffset - 1 } ?: node.endOffset
        val hasCode = codeStart in 1..codeEnd
        if (hasCode) {
            val language = node.children.firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }
                ?.let { text.substring(it.startOffset, it.endOffset).trim() }
            add(CodeContent(codeStart, codeEnd, language))
        }

        if (isEdited(node) || !hasCode) {
            add(CodeBackground(node.startOffset, node.endOffset))
            node.children.filter { it.type in FENCE_SYNTAX }.forEach { look(it, TextLook.Syntax) }
        } else {
            // the fence lines with their line breaks

            add(CodeBackground(codeStart, codeEnd))
            add(Replacement(node.startOffset, codeStart, ""))
            if (closing != null) add(Replacement(codeEnd, node.endOffset, ""))
        }
    }

    private fun atxHeading(node: ASTNode, level: Int) {
        add(HeadingLine(node.startOffset, node.endOffset, level))

        val content = node.children.firstOrNull { it.type == MarkdownTokenTypes.ATX_CONTENT }
        val contentStart = content?.let { firstNonSpace(it.startOffset, it.endOffset) } ?: node.endOffset
        // without any text the hashes stay, or the heading would be an empty line
        if (contentStart < node.endOffset) syntax(node.startOffset, contentStart, node) else look(node, TextLook.Syntax)

        content?.let { inline(it.children) }
    }

    private fun setextHeading(node: ASTNode, level: Int) {
        val underline = node.children.lastOrNull { it.type in SETEXT_UNDERLINES }
        val contentEnd = underline?.let { text.lastIndexOf('\n', it.startOffset).coerceAtLeast(node.startOffset) }
            ?: node.endOffset
        add(HeadingLine(node.startOffset, contentEnd, level))
        underline?.let { look(it, TextLook.Syntax) }
        node.children.filter { it.type == MarkdownTokenTypes.SETEXT_CONTENT }.forEach { inline(it.children) }
    }

    private fun list(node: ASTNode, depth: Int) {
        val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }
        // a numbered list counts from the number of its first item, like the rendered notes - Enter in a list
        // continues it with "1. ", so the numbers written in the note do not have to be in order
        val start = items.firstOrNull()?.let { markerOf(it) }?.takeIf { it.type == MarkdownTokenTypes.LIST_NUMBER }
            ?.let { text.substring(it.startOffset, numberEnd(it)).trim().toIntOrNull() }
        items.forEachIndexed { index, item -> listItem(item, depth, number = start?.plus(index)) }
    }

    private fun markerOf(item: ASTNode): ASTNode? = item.children.firstOrNull { it.type in LIST_MARKERS }

    // where the digits of a number marker end
    private fun numberEnd(marker: ASTNode): Int {
        val start = firstNonSpace(marker.startOffset, marker.endOffset)
        return (start until marker.endOffset).firstOrNull { !text[it].isDigit() } ?: marker.endOffset
    }

    /** An item of a list - [number] is the one it has in its list, null in a list with bullets. */
    private fun listItem(item: ASTNode, depth: Int, number: Int?) {
        val marker = markerOf(item)
        if (marker != null) listMarker(item, marker, depth, number)

        item.children
            .filter { it !== marker && it.type != GFMTokenTypes.CHECK_BOX }
            .forEach { block(it, isTopLevel = false, listDepth = depth + 1) }
    }

    private fun listMarker(item: ASTNode, marker: ASTNode, depth: Int, number: Int?) {
        // the token of a nested item can start with indentation that belongs to the item it is in ("  - " for an
        // item indented by four spaces) - the indentation stays, only the bullet itself is the syntax
        val markerStart = firstNonSpace(marker.startOffset, marker.endOffset)
        val checkbox = item.children.firstOrNull { it.type == GFMTokenTypes.CHECK_BOX }

        when {
            checkbox != null -> task(item, markerStart, checkbox)
            marker.type == MarkdownTokenTypes.LIST_BULLET -> bullet(markerStart, depth)
            else -> number(markerStart, marker, number)
        }

        // the tokens of the bullet and of the box end with the space before the text
        val contentStart = checkbox?.endOffset ?: marker.endOffset
        val lineEnd = text.lineEndAt(markerStart)
        if (contentStart <= lineEnd) add(ListLine(text.lineStartAt(markerStart), lineEnd, contentStart))
    }

    // "1. " - shown with the number the item has in its list, the written one while the cursor is at it
    private fun number(markerStart: Int, marker: ASTNode, number: Int?) {
        add(Look(markerStart, marker.endOffset, TextLook.Syntax))

        val digitsEnd = numberEnd(marker)
        val isWrittenNumber = text.substring(markerStart, digitsEnd) == number.toString()
        // the cursor right after the dot counts as at it
        if (number != null && !isWrittenNumber && !isEdited(markerStart, digitsEnd + 1)) {
            add(Replacement(markerStart, digitsEnd, number.toString()))
        }
    }

    // the same length, so the text does not move when the cursor comes to it
    private fun bullet(bullet: Int, depth: Int) {
        if (isEdited(bullet, bullet + 1)) add(Look(bullet, bullet + 1, TextLook.Syntax))
        else add(Replacement(bullet, bullet + 1, LIST_BULLETS[depth % LIST_BULLETS.size]))
    }

    // "- [ ] " - the bullet and the brackets become one box
    private fun task(item: ASTNode, markerStart: Int, checkbox: ASTNode) {
        val boxEnd = checkbox.startOffset + 3
        val isChecked = text.getOrNull(checkbox.startOffset + 1)?.lowercaseChar() == 'x'
        if (isEdited(markerStart, boxEnd)) {
            add(Look(markerStart, boxEnd, TextLook.Syntax))
        } else {
            add(Replacement(markerStart, checkbox.startOffset, ""))
            add(TaskCheckbox(checkbox.startOffset, boxEnd, isChecked))
        }
        if (isChecked) item.children.firstOrNull { it.type == MarkdownElementTypes.PARAGRAPH }?.let { look(it, TextLook.DoneTask) }
    }

    private fun quote(node: ASTNode, listDepth: Int) {
        val markers = quoteMarkers(node)
        val isEdited = isEdited(node)
        val title = node.children.firstOrNull { it.type == GFMTokenTypes.ALERT_TITLE }

        if (!isEdited && markers.isNotEmpty()) {
            add(QuoteBar(node.startOffset, node.endOffset, markers.first().startOffset, title?.let { AlertKind.of(alertName(it)) }))
        }
        for (marker in markers) {
            // the `>` and the space after it
            val hasSpace = text.getOrNull(marker.endOffset) == ' ' && text[marker.endOffset - 1] != ' '
            val end = if (hasSpace) marker.endOffset + 1 else marker.endOffset
            if (isEdited) {
                add(Look(marker.startOffset, end, TextLook.Syntax))
            } else {
                add(Replacement(marker.startOffset, end, QUOTE_PLACEHOLDER))
                val lineStart = text.lineStartAt(marker.startOffset)
                quoteDepths[lineStart] = (quoteDepths[lineStart] ?: 0) + 1
            }
        }

        for (child in node.children) {
            if (child === title) alertTitle(child, isEdited) else block(child, isTopLevel = false, listDepth = listDepth)
        }
    }

    // [!TIP] - shown as "💡 Tip"
    private fun alertTitle(title: ASTNode, isEdited: Boolean) {
        if (isEdited) return look(title, TextLook.Syntax)

        val name = alertName(title)
        val kind = AlertKind.of(name)
        val label = name.lowercase().replaceFirstChar { it.uppercaseChar() }
        add(Replacement(title.startOffset, title.endOffset, kind?.let { "${it.emoji} $label" } ?: label))
        add(AlertTitle(title.startOffset, title.endOffset, kind))
    }

    private fun alertName(title: ASTNode): String =
        text.substring(title.startOffset, title.endOffset).removePrefix("[!").removeSuffix("]")

    // the `>` of every line of this quote - a nested quote has its own
    private fun quoteMarkers(node: ASTNode): List<ASTNode> = node.children.flatMap { child ->
        when (child.type) {
            MarkdownTokenTypes.BLOCK_QUOTE -> listOf(child)
            MarkdownElementTypes.BLOCK_QUOTE -> emptyList()
            else -> quoteMarkers(child)
        }
    }

    // --- inline ---

    private fun inline(nodes: List<ASTNode>) = nodes.forEach { inlineNode(it) }

    private fun inlineNode(node: ASTNode) {
        when (node.type) {
            MarkdownElementTypes.STRONG -> delimited(node, TextLook.Bold)
            MarkdownElementTypes.EMPH -> delimited(node, TextLook.Italic)
            GFMElementTypes.STRIKETHROUGH -> delimited(node, TextLook.Strikethrough)
            GFMElementTypes.HIGHLIGHT -> delimited(node, TextLook.Highlight)
            // the code is not formatted any further
            MarkdownElementTypes.CODE_SPAN -> delimited(node, TextLook.Code, CODE_DELIMITERS, formatsContent = false)
            GFMElementTypes.INLINE_MATH, GFMElementTypes.BLOCK_MATH -> inlineMath(node)
            MarkdownElementTypes.INLINE_LINK, MarkdownElementTypes.FULL_REFERENCE_LINK -> link(node)
            // <https://example.com>
            MarkdownElementTypes.AUTOLINK -> {
                node.children.filter { it.type == MarkdownTokenTypes.LT || it.type == MarkdownTokenTypes.GT }
                    .forEach { syntax(it.startOffset, it.endOffset, node) }
                autolink(node, isEdited(node))
            }
            // <me@example.com>, https://example.com in text
            MarkdownTokenTypes.EMAIL_AUTOLINK, GFMTokenTypes.GFM_AUTOLINK -> autolink(node, isEdited = false)
            MarkdownTokenTypes.HTML_TAG -> look(node, TextLook.Syntax)
            // an image inside text keeps its source, only a paragraph of its own becomes a picture
            MarkdownElementTypes.IMAGE -> look(node, TextLook.Syntax)
            else -> inline(node.children)
        }
    }

    // **bold**, *italic*, ~~deleted~~, ==highlighted==, `code` - the formatting, with its markers on both sides
    private fun delimited(
        node: ASTNode,
        look: TextLook,
        delimiters: Set<IElementType> = FORMATTING_DELIMITERS,
        formatsContent: Boolean = true,
    ) {
        val leading = node.children.takeWhile { it.type in delimiters }
        val trailing = node.children.drop(leading.size).takeLastWhile { it.type in delimiters }
        val contentStart = leading.lastOrNull()?.endOffset ?: node.startOffset
        val contentEnd = trailing.firstOrNull()?.startOffset ?: node.endOffset
        if (contentStart >= contentEnd) return

        add(Look(contentStart, contentEnd, look))
        syntax(node.startOffset, contentStart, node)
        syntax(contentEnd, node.endOffset, node)
        if (formatsContent) inline(node.children.drop(leading.size).dropLast(trailing.size))
    }

    // $x^2$ - a formula, its source while the cursor is at it
    private fun inlineMath(node: ASTNode) {
        val latex = node.latexSource(text)
        if (isEdited(node) || latex.isEmpty()) look(node, TextLook.Math)
        else add(InlineMath(node.startOffset, node.endOffset, latex))
    }

    // [text](destination) - only the text stays, looking like a link
    private fun link(node: ASTNode) {
        val linkText = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
        if (linkText == null || linkText.endOffset - linkText.startOffset < 2) return inline(node.children)

        val textStart = linkText.startOffset + 1
        val textEnd = linkText.endOffset - 1
        add(Look(textStart, textEnd, TextLook.Link))
        syntax(node.startOffset, textStart, node)
        syntax(textEnd, node.endOffset, node)
        node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }?.let { destination ->
            add(LinkTarget(textStart, textEnd, linkUrl(destination.address()), isEdited(node)))
        }
        inline(linkText.children)
    }

    // an address written as a link - the address is the link's text
    private fun autolink(node: ASTNode, isEdited: Boolean) {
        look(node, TextLook.Link)
        add(LinkTarget(node.startOffset, node.endOffset, linkUrl(node.address()), isEdited))
    }

    // the address a link node holds, without the <> it may be written in
    private fun ASTNode.address(): String = text.substring(startOffset, endOffset).removeSurrounding("<", ">")
}

// what a link opens: an address without a scheme is a web page, or an e-mail
private fun linkUrl(address: String): String = when {
    address.contains("://") || address.startsWith("mailto:") -> address
    address.contains('@') && !address.contains('/') -> "mailto:$address"
    address.startsWith("www.") -> "https://$address"
    else -> address
}
