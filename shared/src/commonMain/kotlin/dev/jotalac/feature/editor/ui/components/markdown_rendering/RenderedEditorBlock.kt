package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.parseMarkdown
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.editor.ui.utils.getHeaderFontSize
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.delete_block_content_description
import git_writer.shared.generated.resources.x_icon
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser
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

    Box(
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
            )
        } else {
            RenderedMarkdownBlock(
                text = text,
                onTextChange = onTextChange,
            )
        }

        // the delete button
        if (isDesktopPlatform) {
            Icon(
                painter = painterResource(Res.drawable.x_icon),
                contentDescription = stringResource(Res.string.delete_block_content_description),
                modifier = Modifier
                    .alpha(if (isHovered) 1f else 0f)
                    .dropShadow(
                        shape = RectangleShape,
                        shadow = Shadow(
                            radius = 10.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            spread = 5.dp
                        )
                    )
                    .align(AbsoluteAlignment.TopRight)
                    .padding(5.dp)
                    .size(MaterialTheme.dimensions.iconMedium)
                    .clickable(onClick = onDeleteClick),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun RenderedMarkdownBlock(
    text: String,
    modifier: Modifier = Modifier,
    onTextChange: (String) -> Unit = {},
) {
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

    val markdownFlavour = remember { GFMFlavourDescriptor() }
    val markdownParser = remember(markdownFlavour) {
        MarkdownParser(markdownFlavour, cancellationToken = CancellationToken.NonCancellable)
    }
    val markdownReferenceLinkHandler = remember { ReferenceLinkHandlerImpl() }

    val markdownState = remember(currentText, markdownFlavour, markdownParser, markdownReferenceLinkHandler) {
        parseMarkdown(
            content = currentText,
            flavour = markdownFlavour,
            parser = markdownParser,
            referenceLinkHandler = markdownReferenceLinkHandler,
        )
    }

    Markdown(
        state = markdownState,
        modifier = modifier,
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
            listItemBottom = 2.dp,
        ),
    )
}
