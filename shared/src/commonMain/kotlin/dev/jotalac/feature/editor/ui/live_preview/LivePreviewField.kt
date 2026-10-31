package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.jotalac.core.utils.getImageBytesFromClipboard
import dev.jotalac.core.utils.requestFocusSafely
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import dev.jotalac.feature.editor.ui.spellcheck.LocalSpellcheckEnabled
import dev.jotalac.feature.editor.ui.spellcheck.SpellcheckMenuHost
import dev.jotalac.feature.editor.ui.spellcheck.drawMisspellingUnderlines
import dev.jotalac.feature.editor.ui.spellcheck.onSpellingMenuRequest
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.spellcheck_suggestions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * The whole note in one text field, shown as a live preview: formatted, with the markdown syntax hidden except
 * where the cursor is, and images, tables and maths drawn over the space kept for them.
 */
@Composable
fun LivePreviewField(
    editorState: MarkdownEditorState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.bodyLarge
    val value = editorState.value
    val text = value.text

    var isPointerDown by remember { mutableStateOf(false) }
    val selection = rememberShownSelection(editorState, isPointerDown)

    val tree = remember(text) { parseNoteMarkdown(text) }
    val decorations = remember(tree, selection) { previewDecorations(text, tree, selection) }
    val styles = rememberPreviewStyles(textStyle)
    val widgetHeights = remember { WidgetHeights() }
    val knownHeights = widgetHeights.current()

    // preview of the text with the styling
    val preview = remember(text, decorations, knownHeights, styles, density) {
        buildLivePreviewText(text, decorations, styles) { widget -> widgetLineHeight(widget, text, knownHeights, density) }
    }

    // Set by the text field while it is measured - the overlays and the drawings read it in the same frame, right
    // after, so they are never a frame behind the text, which moves with every typed character.
    val shownLayout = remember { LayoutHolder() }
    // the same, as state - for what is not drawn or placed (the scrolling, the keys, the spell check menu)
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val layout = textLayout?.takeIf { it.isLayoutOf(preview) }

    val focusRequester = rememberFieldFocusRequester(editorState)
    val spellcheck = rememberPreviewSpellcheck(editorState)
    val underlines = spellcheck.underlines(text, decorations, preview.offsetMapping)
    val suggestionsHeader = stringResource(Res.string.spellcheck_suggestions)
    val isSpellcheckEnabled = LocalSpellcheckEnabled.current

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(value.selection, layout, editorState.isFocused) {
        if (!editorState.isFocused || layout == null) return@LaunchedEffect
        val cursorRect = layout.getCursorRect(preview.offsetMapping.originalToTransformed(value.selection.end))
        bringIntoViewRequester.bringIntoView(cursorRect.copy(top = cursorRect.top - 40f, bottom = cursorRect.bottom + 40f))
    }

    val uriHandler = LocalUriHandler.current
    var isOverLink by remember { mutableStateOf(false) }
    // read by the pointer handling, which lives longer than one composition
    val currentPreview by rememberUpdatedState(preview)

    val cursorMove = remember { LineCursorMove() }
    fun handleKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        if (isContextMenuShortcut(event)) {
            spellcheck.openMenuAtCaret(layout, preview.offsetMapping)
            return true
        }
        val movedSelection = layout?.let { cursorMove.selectionFor(event, value.selection, it, preview.offsetMapping, text.length) }
        if (movedSelection != null) {
            editorState.updateValue(value.copy(selection = movedSelection))
            return true
        }
        return handleEditorKey(event, editorState) { pasteClipboardImage(editorState, scope) }
    }

    val colors = MaterialTheme.colorScheme

    Layout(
        modifier = modifier,
        measurePolicy = rememberOverlayMeasurePolicy(preview, shownLayout),
        content = {
            SpellcheckMenuHost(
                items = { if (isSpellcheckEnabled) spellcheck.menuItems(suggestionsHeader) else emptyList() },
                controller = spellcheck.menuController,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = editorState::onValueChange,
                    onTextLayout = {
                        shownLayout.layout = it
                        textLayout = it
                    },
                    visualTransformation = remember(preview) { LivePreviewTransformation(preview) },
                    textStyle = textStyle.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .previewPointerInput(
                            linkAt = { position, keyboardModifiers ->
                                shownLayout.forText(currentPreview)?.let { currentPreview.linkAt(it, position, keyboardModifiers) }
                            },
                            onPointerDownChange = { isPointerDown = it },
                            onOverLinkChange = { isOverLink = it },
                            onLinkClick = { link -> openLink(link, uriHandler::openUri) },
                        )
                        .pointerHoverIcon(if (isOverLink) PointerIcon.Hand else PointerIcon.Text, overrideDescendants = true)
                        .drawBehind {
                            val drawnLayout = shownLayout.forText(preview) ?: return@drawBehind
                            drawCodeBackgrounds(drawnLayout, preview.codeBackgrounds, colors.surfaceContainerHigh)
                            drawQuoteBars(drawnLayout, preview.quoteBars, colors.outlineVariant)
                        }
                        .drawWithContent {
                            drawContent()
                            shownLayout.forText(preview)?.let { drawMisspellingUnderlines(it, underlines, colors.error) }
                        }
                        .onSpellingMenuRequest { position ->
                            spellcheck.menuAnchor = layout?.let {
                                preview.offsetMapping.transformedToOriginal(it.getOffsetForPosition(position))
                            }
                        }
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .focusRequester(focusRequester)
                        .onFocusChanged { editorState.isFocused = it.isFocused }
                        .onPreviewKeyEvent(::handleKey),
                )
            }

            PreviewOverlays(
                items = rememberOverlayItems(preview),
                onWidgetHeight = widgetHeights::record,
                onWidgetClick = { widget -> editorState.focusAt(widget.start) },
                onWidgetSourceChange = { widget, newSource -> editorState.replaceRange(widget.start, widget.end, newSource) },
                onFormulaClick = { math -> editorState.focusAt(math.start) },
                onCheckboxClick = { checkbox ->
                    editorState.replaceRange(checkbox.start + 1, checkbox.start + 2, if (checkbox.isChecked) " " else "x")
                },
            )
        },
    )
}

/** The layout of the shown text, kept by the text field while it is measured - see [LivePreviewField]. */
internal class LayoutHolder {
    var layout: TextLayoutResult? = null

    /** The layout when it is the one of [preview]'s text. */
    fun forText(preview: LivePreviewText): TextLayoutResult? = layout?.takeIf { it.isLayoutOf(preview) }
}

private fun TextLayoutResult.isLayoutOf(preview: LivePreviewText): Boolean =
    layoutInput.text.text == preview.transformed.text.text

/**
 * The selection the syntax is shown at - none while the field is not focused, so the whole note is a preview.
 * While a pointer is down it stays the one from before: the text would move under the pointer, and the pointer
 * would select somewhere else in the moved text
 **/

@Composable
private fun rememberShownSelection(editorState: MarkdownEditorState, isPointerDown: Boolean): TextRange? {
    val shown = remember { ShownSelection() }
    if (!isPointerDown) shown.selection = editorState.value.selection.takeIf { editorState.isFocused }
    return shown.selection
}

private class ShownSelection {
    var selection: TextRange? = null
}

@Composable
private fun rememberFieldFocusRequester(editorState: MarkdownEditorState): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    DisposableEffect(editorState, focusRequester) {
        editorState.requestFieldFocus = { focusRequester.requestFocusSafely() }
        onDispose { editorState.requestFieldFocus = null }
    }
    return focusRequester
}

private fun openLink(link: LinkTarget, openUri: (String) -> Unit) {
    try {
        openUri(link.url)
    } catch (_: Exception) {
        // not an address the system can open - each platform fails with its own exception
    }
}

private fun pasteClipboardImage(editorState: MarkdownEditorState, scope: CoroutineScope) {
    scope.launch(Dispatchers.Default) {
        val imageBytes = getImageBytesFromClipboard() ?: return@launch
        withContext(Dispatchers.Main) { editorState.pasteImages(listOf(imageBytes)) }
    }
}

/** Shows the preview, built for exactly this text - any other text (it never should) is shown as it is. */
private class LivePreviewTransformation(private val preview: LivePreviewText) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        if (text.text == preview.source) preview.transformed else TransformedText(text, OffsetMapping.Identity)
}
