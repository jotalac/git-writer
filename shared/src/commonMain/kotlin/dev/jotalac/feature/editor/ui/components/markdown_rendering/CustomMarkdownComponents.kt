package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.LatexAutoWrap
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.mikepenz.markdown.annotator.AnnotatorSettings
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.model.DefaultMarkdownAnnotator
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownImage
import com.mikepenz.markdown.compose.elements.MarkdownListItems
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.compose.elements.listDepth
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

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

// unordered list
@Composable
fun CustomUnorderedListComponent(model: MarkdownComponentModel) {
    MarkdownListItems(
        content = model.content,
        node = model.node,
        depth = model.listDepth,
        bullet = { startNumber, index, child ->
            when (model.listDepth) {
                0 -> Text("• ")
                1 -> Text("◦ ")
                2 -> Text("▪ ")
                else -> Text("▫ ")
            }
        }
    )
}

// ordered list
@Composable
fun CustomOrderedListComponent(model: MarkdownComponentModel) {
    MarkdownListItems(
        content = model.content,
        node = model.node,
        depth = model.listDepth,
        bullet = { index, _, _ ->
            val color = when (model.listDepth % 3) {
                0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                1 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            }
            Text("${index + 1}.", color = color)
        }
    )
}

// paragraph with latex block
@Composable
fun CustomParagraphComponent(model: MarkdownComponentModel) {
    val mathNodes = model.node.children.filter { it.type.name in MATH_NODE_TYPES }

    // a paragraph that is one expression and nothing else is a maths block: draw it as latex
    if (mathNodes.isLoneExpression(model)) {
        LatexExpression(model.content, mathNodes.single())
        return
    }

    if (mathNodes.isEmpty()) {
        MarkdownParagraph(content = model.content, node = model.node)
        return
    }

    // pragraph with maths in it: the library would drop the expression, so put its source back
    // later version will be able to render the math expression inside the paragraph
    MarkdownParagraph(
        content = model.content,
        node = model.node,
        annotatorSettings = keepMathSourceSettings()
    )
}

// keep the math source inside the paragraph
@Composable
private fun keepMathSourceSettings(): AnnotatorSettings {
    val librarySettings = annotatorSettings()

    return annotatorSettings(
        annotator = DefaultMarkdownAnnotator(
            // the receiver is the AnnotatedString.Builder being filled
            annotate = { content, node ->
                if (node.type.name in MATH_NODE_TYPES) {
                    append(node.getTextInNode(content))
                    true
                } else {
                    librarySettings.annotator.annotate?.invoke(this, content, node) ?: false
                }
            },
            config = librarySettings.annotator.config
        )
    )
}

@Composable
private fun LatexExpression(content: String, mathNode: ASTNode) {
    LatexAutoWrap(mathNode.latexSource(content), config = latexConfig())
}

// --- maths ---------------------------------------------------------------------------------------

private val MATH_NODE_TYPES = setOf("BLOCK_MATH", "INLINE_MATH")

// True when the paragraph is one expression and nothing else - no words to sit beside.
private fun List<ASTNode>.isLoneExpression(model: MarkdownComponentModel): Boolean =
    size == 1 && model.node.children.none { it !== single() && it.hasText(model) }

// The latex source of an expression, without the `$` delimiters markdown wraps it in.
private fun ASTNode.latexSource(content: String): String =
    getTextInNode(content).toString().removeSurrounding("$$").removeSurrounding("$").trim()

private fun ASTNode.hasText(model: MarkdownComponentModel): Boolean =
    getTextInNode(model.content).isNotBlank()

@Composable
private fun latexConfig(): LatexConfig = LatexConfig(theme = LatexTheme.material3())

// custom image component
@Composable
fun CustomImageComponent(model: MarkdownComponentModel) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
        MarkdownImage(content = model.content, node = model.node)
    }
}