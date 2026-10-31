package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.parseMarkdown
import dev.jotalac.feature.editor.ui.utils.getHeaderFontSize
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

/** One markdown block rendered - used to render notes for the PDF export. */
@Composable
internal fun RenderedMarkdownBlock(
    text: String,
    modifier: Modifier = Modifier,
    onTextChange: (String) -> Unit = {},
    darkCodeTheme: Boolean = isSystemInDarkTheme(),
) {
    val highlightsBuilder = remember(darkCodeTheme) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = darkCodeTheme))
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
                CustomParagraphComponent(model, onImageResize = { currentOnTextChange(it) })
            },
            checkbox = customCheckboxComponent { offset, isChecked ->
                val newCheckbox = if (isChecked) "[x]" else "[ ]"
                currentOnTextChange(currentText.replaceRange(offset, offset + 3, newCheckbox))
            },
            heading1 = customHeadingComponent { it.h1 },
            heading2 = customHeadingComponent { it.h2 },
            heading3 = customHeadingComponent { it.h3 },
            heading4 = customHeadingComponent { it.h4 },
            heading5 = customHeadingComponent { it.h5 },
            heading6 = customHeadingComponent { it.h6 },
            unorderedList = { CustomUnorderedListComponent(it) },
            orderedList = { CustomOrderedListComponent(it) },
            image = { CustomImageComponent(it, onTextChange = { newText -> currentOnTextChange(newText) }) },
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

    // markdown that would draw nothing shows its raw text, instead of an empty block
    val drawsAnything = remember(markdownState) {
        (markdownState as? State.Success)?.let { it.node.drawsAnything(it.content) } ?: true
    }
    if (!drawsAnything) {
        Text(
            text = currentText,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
        )
        return
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
