package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.LatexAutoWrap
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import dev.jotalac.core.ui.theme.dimensions
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.x_icon
import org.intellij.markdown.ast.getTextInNode
import org.jetbrains.compose.resources.painterResource

@Composable
fun RenderedEditorBlock(
    text: String,
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit,
    onTextChange: (String) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        // the displayed text content
        if (text.isBlank()) {
            Text(
                text = " ",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        } else {
            val isDarkTheme = isSystemInDarkTheme()
            val highlightsBuilder = remember(isDarkTheme) {
                Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
            }

            val currentText by rememberUpdatedState(text)
            val currentOnTextChange by rememberUpdatedState(onTextChange)

            val customMarkdownComponents = remember(highlightsBuilder) {
                markdownComponents(
                    codeFence = {
                        MarkdownHighlightedCodeFence(
                            content = it.content,
                            node = it.node,
                            highlightsBuilder = highlightsBuilder,
                            showHeader = true
                        )
                    },
                    paragraph = { model ->
                        if (!renderedLatexInParagraph(model, isDarkTheme)) {
                            MarkdownParagraph(
                                content = model.content,
                                node = model.node
                            )
                        }
                    },
                    checkbox = customCheckboxComponent { offset, isChecked ->
                        val newCheckbox = if (isChecked) "[x]" else "[ ]"
                        currentOnTextChange(currentText.replaceRange(offset, offset + 3, newCheckbox))
                    }
                )
            }

            Markdown(
                content = currentText,
                modifier = Modifier.weight(1f),
                components = customMarkdownComponents,
                imageTransformer = Coil3ImageTransformerImpl,
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    h2 = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    h3 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    h4 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    h5 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    h6 = MaterialTheme.typography.titleMedium,

                    )
            )
        }

        // the delete button
        Icon(
            painter = painterResource(Res.drawable.x_icon),
            contentDescription = "Delete Block",
            modifier = Modifier
                .alpha(if (isHovered) 1f else 0f)
                .padding(start = 10.dp)
                .size(MaterialTheme.dimensions.iconMedium)
                .clickable(onClick = onDeleteClick),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun renderedLatexInParagraph(model: MarkdownComponentModel, isDarkTheme: Boolean): Boolean {
    val mathChild = model.node.children.find { it.type.name in listOf("BLOCK_MATH", "INLINE_MATH") }
    return if (mathChild != null) {
        val mathText = mathChild.getTextInNode(model.content).toString()
            .removeSurrounding("$$")
            .removeSurrounding("$").trim()
        LatexAutoWrap(
            mathText,
            config = LatexConfig(
                theme = LatexTheme.material3()
            )
        )
        true
    } else {
        false
    }
}