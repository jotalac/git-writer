package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.runtime.Composable
import com.hrm.latex.renderer.LatexAutoWrap
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.mikepenz.markdown.annotator.AnnotatorSettings
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.model.DefaultMarkdownAnnotator
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

/*
* Maths in a paragraph: `$x^2$` inline or `$$...$$` as a block. A paragraph with nothing but an expression
* is drawn as latex, maths inside text keeps its source until it can be rendered there too.
* */

private val MATH_NODE_TYPES = setOf("BLOCK_MATH", "INLINE_MATH")

private val ASTNode.isMath: Boolean
    get() = type.name in MATH_NODE_TYPES

internal fun ASTNode.containsMath(): Boolean = children.any { it.isMath }

/** The expression when it is the only thing in this paragraph - no words to sit beside. */
internal fun ASTNode.loneMathExpression(content: String): ASTNode? =
    textChildren(content).singleOrNull()?.takeIf { it.isMath }

@Composable
internal fun LatexExpression(content: String, mathNode: ASTNode) {
    LatexAutoWrap(mathNode.latexSource(content), config = LatexConfig(theme = LatexTheme.material3()))
}

/** The library's annotator, except maths is kept as its source text instead of being dropped. */
@Composable
internal fun keepMathSourceSettings(): AnnotatorSettings {
    val librarySettings = annotatorSettings()

    return annotatorSettings(
        annotator = DefaultMarkdownAnnotator(
            // the receiver is the AnnotatedString.Builder being filled
            annotate = { content, node ->
                if (node.isMath) {
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

/** The latex source of an expression, without the `$` delimiters markdown wraps it in. */
internal fun ASTNode.latexSource(content: String): String =
    getTextInNode(content).toString().removeSurrounding("$$").removeSurrounding("$").trim()
