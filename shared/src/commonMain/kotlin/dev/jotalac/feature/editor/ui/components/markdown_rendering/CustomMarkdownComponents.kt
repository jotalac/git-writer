package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownHeader
import com.mikepenz.markdown.compose.elements.MarkdownListItems
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.compose.elements.listDepth
import com.mikepenz.markdown.model.MarkdownTypography
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

/*
* The editor's own versions of the markdown library's components, used by RenderedEditorBlock.
* Images live in CustomImageComponent.kt, maths in MarkdownMath.kt.
* */

// checkbox
fun customCheckboxComponent(onCheckedChange: (Int, Boolean) -> Unit): MarkdownComponent = { model ->
    val text = model.node.getTextInNode(model.content).toString()
    val checked = text.contains("[x]", ignoreCase = true)

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(model.node.startOffset, !checked) },
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

// a different bullet for every nesting level, the deepest one repeats
private val BULLETS = listOf("• ", "◦ ", "▪ ", "▫ ")

// unordered list
@Composable
fun CustomUnorderedListComponent(model: MarkdownComponentModel) {
    MarkdownListItems(
        content = model.content,
        node = model.node,
        depth = model.listDepth,
        bullet = { _, _, _ ->
            Text(BULLETS[model.listDepth % BULLETS.size])
        }
    )
}

// the numbers fade with every nesting level, then start over
private val NUMBER_ALPHAS = listOf(0.8f, 0.6f, 0.4f)

// ordered list
@Composable
fun CustomOrderedListComponent(model: MarkdownComponentModel) {
    MarkdownListItems(
        content = model.content,
        node = model.node,
        depth = model.listDepth,
        bullet = { index, _, _ ->
            val alpha = NUMBER_ALPHAS[model.listDepth % NUMBER_ALPHAS.size]
            Text("${index + 1}.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        }
    )
}

// heading - without any text after the hashes the library draws an empty line, so the hashes stay visible instead
fun customHeadingComponent(style: (MarkdownTypography) -> TextStyle): MarkdownComponent = { model ->
    val headingStyle = style(model.typography)

    if (model.node.hasHeadingText(model.content)) {
        MarkdownHeader(model.content, model.node, style = headingStyle)
    } else {
        Text(
            text = model.node.getTextInNode(model.content).toString().trim(),
            style = headingStyle,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

internal fun ASTNode.hasHeadingText(content: String): Boolean =
    findChildOfType(MarkdownTokenTypes.ATX_CONTENT)?.getTextInNode(content)?.isNotBlank() == true

// paragraph - plain text, or a block of its own when it holds nothing but an image or a maths expression.
// [onImageResize] changes the block's text when its image is resized, see CustomImageComponent
@Composable
fun CustomParagraphComponent(
    model: MarkdownComponentModel,
    onImageResize: ((String) -> Unit)? = null,
) {
    val content = model.content
    val node = model.node

    // the library decides by the image's size - one smaller than 1.5 lines of text would stay inline
    // and ignore the size in its alt text (![photo|300](...))
    node.loneImage(content)?.let { image ->
        CustomImageComponent(model.copy(node = image), onImageResize)
        return
    }

    node.loneMathExpression(content)?.let { expression ->
        LatexExpression(content, expression)
        return
    }

    MarkdownParagraph(
        content = content,
        node = node,
        // maths inside text is not rendered yet - without this the library would drop the expression
        annotatorSettings = if (node.containsMath()) keepMathSourceSettings() else annotatorSettings(),
    )
}
