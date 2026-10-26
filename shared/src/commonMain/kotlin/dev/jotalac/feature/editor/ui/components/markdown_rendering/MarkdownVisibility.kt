package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import com.mikepenz.markdown.annotator.DefaultAnnotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.model.markdownAnnotator
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes

/*
* Whether a block draws anything at all - a block that would stay empty (only a link definition, an HTML comment,
* an empty quote, a link without text, ...) shows its raw text instead, see RenderedMarkdownBlock.
* The text of a paragraph is built by the markdown library itself, so its inline rules are not repeated here.
* A wrong "nothing" only shows the raw text, which is never worse than an empty block.
* */

// drawn even without any text
private val ALWAYS_DRAWN_BLOCKS = setOf(
    // an empty one shows its hashes, see customHeadingComponent
    MarkdownElementTypes.ATX_1,
    MarkdownElementTypes.ATX_2,
    MarkdownElementTypes.ATX_3,
    MarkdownElementTypes.ATX_4,
    MarkdownElementTypes.ATX_5,
    MarkdownElementTypes.ATX_6,
    MarkdownElementTypes.SETEXT_1,
    MarkdownElementTypes.SETEXT_2,
    MarkdownElementTypes.CODE_FENCE,
    MarkdownElementTypes.CODE_BLOCK,
    // the bullets and numbers
    MarkdownElementTypes.ORDERED_LIST,
    MarkdownElementTypes.UNORDERED_LIST,
    MarkdownElementTypes.IMAGE,
    MarkdownTokenTypes.HORIZONTAL_RULE,
    GFMElementTypes.TABLE,
    GFMElementTypes.ALERT,
)

// only whether there is any text matters, not how it looks
private val plainAnnotatorSettings = DefaultAnnotatorSettings(
    linkTextSpanStyle = TextLinkStyles(),
    codeSpanStyle = SpanStyle(),
    annotator = markdownAnnotator(),
    highlightSpanStyle = SpanStyle(),
)

internal fun ASTNode.drawsAnything(content: String): Boolean = when (type) {
    in ALWAYS_DRAWN_BLOCKS -> true
    // images and maths leave a placeholder or their source in the text, so they count too
    MarkdownElementTypes.PARAGRAPH -> buildAnnotatedString {
        buildMarkdownAnnotatedString(content, this@drawsAnything, plainAnnotatorSettings)
    }.isNotBlank()
    MarkdownTokenTypes.TEXT -> hasText(content)
    MarkdownElementTypes.LINK_DEFINITION -> false
    // quotes, HTML blocks and the root - drawn when something inside is
    else -> children.any { it.drawsAnything(content) }
}
