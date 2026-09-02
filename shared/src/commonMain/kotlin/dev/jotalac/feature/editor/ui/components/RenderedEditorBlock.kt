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
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.editor.ui.utils.getHeaderFontSize
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.delete_block_content_description
import git_writer.shared.generated.resources.x_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
            .padding(horizontal = 8.dp, vertical = 8.dp)
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
                            showHeader = true,
                        )
                    },
                    paragraph = { model ->
                        CustomParagraphComponent(model)
                    },
                    checkbox = customCheckboxComponent { offset, isChecked ->
                        val newCheckbox = if (isChecked) "[x]" else "[ ]"
                        currentOnTextChange(currentText.replaceRange(offset, offset + 3, newCheckbox))
                    },
                    unorderedList = { CustomUnorderedListComponent(it) },
                    image = { CustomImageComponent(it) },
                )
            }

            Markdown(
                content = currentText,
                modifier = Modifier.weight(1f),
                components = customMarkdownComponents,
                imageTransformer = Coil3ImageTransformerImpl,
                typography = markdownTypography(
                    h1 = getHeaderFontSize(1),
                    h2 = getHeaderFontSize(2),
                    h3 = getHeaderFontSize(3),
                    h4 = getHeaderFontSize(4),
                    h5 = getHeaderFontSize(5),
                    h6 = getHeaderFontSize(6)
                ),
                padding = markdownPadding(
                    listItemBottom = 1.dp,
                ),

                )
        }

        // the delete button
        if (isDesktopPlatform) {
            Icon(
                painter = painterResource(Res.drawable.x_icon),
                contentDescription = stringResource(Res.string.delete_block_content_description),
                modifier = Modifier
                    .alpha(if (isHovered) 1f else 0f)
                    .padding(start = 10.dp)
                    .size(MaterialTheme.dimensions.iconMedium)
                    .clickable(onClick = onDeleteClick),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

