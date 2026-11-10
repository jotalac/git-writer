package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.LocalImageTransformer
import com.mikepenz.markdown.compose.LocalReferenceLinkHandler
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.model.ReferenceLinkHandler
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import dev.jotalac.core.ui.components.ContextMenuHeader
import dev.jotalac.core.ui.components.ContextMenuItem
import dev.jotalac.core.ui.components.DesktopContextMenu
import dev.jotalac.core.ui.components.onContextMenuOpen
import dev.jotalac.core.utils.isDesktopPlatform
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.delete
import git_writer.shared.generated.resources.image_remove
import git_writer.shared.generated.resources.image_size_header
import git_writer.shared.generated.resources.image_size_original
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * An image drawn in markdown note
 * Using obsidian-like image sizes - ![alttext|WidthxHeight](source)
 */
@Composable
fun CustomImageComponent(
    model: MarkdownComponentModel,
    onTextChange: ((String) -> Unit)? = null,
) {
    val link = model.node.imageLink(model.content, LocalReferenceLinkHandler.current) ?: return
    val alt = parseImageAlt(model.node.imageAltText(model.content))
    val imageTransformer = LocalImageTransformer.current
    val imageData = imageTransformer.transform(link) ?: return
    // the size of the loaded picture, unspecified until it has loaded
    val pictureSize = imageTransformer.intrinsicSize(imageData.painter)

    var menuPosition by remember { mutableStateOf<DpOffset?>(null) }
    val haptic = LocalHapticFeedback.current
    val menuModifier = when {
        onTextChange == null -> Modifier
        isDesktopPlatform -> Modifier.onContextMenuOpen { menuPosition = it }
        else -> Modifier.onLongPress {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuPosition = DpOffset.Zero
        }
    }

    Box {
        Image(
            painter = imageData.painter,
            contentDescription = alt.text ?: imageData.contentDescription,
            modifier = imageData.modifier
                .imageSize(alt, pictureSize)
                .clip(RoundedCornerShape(8.dp))
                .then(menuModifier),
            alignment = imageData.alignment,
            contentScale = if (alt.isStretched) ContentScale.FillBounds else imageData.contentScale,
            alpha = imageData.alpha,
            colorFilter = imageData.colorFilter,
        )

        menuPosition?.let { position ->
            // anchored at the click, so the menu opens there however tall the image is
            Box(Modifier.offset(position.x, position.y)) {
                ImageContextMenu(
                    onDismissRequest = { menuPosition = null },
                    onSizeSelected = { width ->
                        model.node.resizedIn(model.content, width)?.let { onTextChange?.invoke(it) }
                    },
                    onImageDeleted = { onTextChange?.invoke("") },
                    // the scales need the picture's size, known once it has loaded
                    pictureWidth = pictureSize.takeIf { it.hasArea() }?.width,
                )
            }
        }
    }
}

// a menu at the pointer on desktop, a sheet from the bottom on a phone
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageContextMenu(
    onDismissRequest: () -> Unit,
    onSizeSelected: (width: Int?) -> Unit,
    onImageDeleted: () -> Unit,
    pictureWidth: Float?,
) {
    val items: @Composable () -> Unit = {
        if (pictureWidth != null) {
            ContextMenuHeader(stringResource(Res.string.image_size_header))
            IMAGE_SCALES.forEach { scale ->
                ContextMenuItem(
                    text = scale.scaleLabel(),
                    onClick = { onSizeSelected(scaledImageWidth(pictureWidth, scale)) },
                    onDismissRequest = onDismissRequest,
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 4.dp),
            )
        }
        ContextMenuItem(
            text = stringResource(Res.string.image_remove),
            iconPainter = Res.drawable.delete,
            onClick = onImageDeleted,
            onDismissRequest = onDismissRequest,
            itemColor = MaterialTheme.colorScheme.error,
        )
    }

    if (isDesktopPlatform) {
        DesktopContextMenu(expanded = true, onDismissRequest = onDismissRequest, offset = DpOffset.Zero, content = items)
    } else {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 24.dp)) { items() }
        }
    }
}

// a long press on image on phone
private fun Modifier.onLongPress(onLongPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (awaitLongPressOrCancellation(down.id) != null) {
            onLongPress()
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

// --- resizing ------------------------------------------------------------------------------------

/** The sizes offered in the image's menu, relative to the picture's own size. */
internal val IMAGE_SCALES = listOf(0.1f, 0.25f, 0.5f, 1f, 2f, 5f)

// width of real pixel - min size is 1
internal fun scaledImageWidth(pictureWidth: Float, scale: Float): Int =
    (pictureWidth * scale).roundToInt().coerceAtLeast(1)

// "0.5x", "2x" - the picture's own size is named, it is also how the picture shows without a size
@Composable
private fun Float.scaleLabel(): String = when {
    this == 1f -> "1x (" + stringResource(Res.string.image_size_original) + ")"
    this % 1f == 0f -> toInt().toString() + "x"
    else -> toString() + "x"
}

/**
 * The block text with this image's size set to [width] - a width only, so it keeps its proportions -
 * or without a size for null. Null when the image has no alt text brackets to write into.
 */
internal fun ASTNode.resizedIn(content: String, width: Int?): String? {
    val linkText = findDescendant(MarkdownElementTypes.LINK_TEXT) ?: return null
    // the text as written between the brackets, so escaped characters stay escaped
    val rawAlt = content.substring(linkText.startOffset + 1, linkText.endOffset - 1)
    val alt = parseImageAlt(rawAlt).copy(width = width, height = null)
    return content.replaceRange(linkText.startOffset, linkText.endOffset, "[${alt.toAltText()}]")
}

/** The image node when it is the only thing in this paragraph. */
internal fun ASTNode.loneImage(content: String): ASTNode? =
    textChildren(content).singleOrNull()?.takeIf { it.type == MarkdownElementTypes.IMAGE }

// --- size ----------------------------------------------------------------------------------------

// wider than any screen, so the editor width still decides - layout constraints cannot hold sizes of
// millions of pixels and crash, so ![photo|1000000](...) must not get that far
private const val MAX_IMAGE_SIDE_DP = 10_000

private fun Int.toImageSide(): Dp = coerceAtMost(MAX_IMAGE_SIDE_DP).dp

// never wider than the editor - the incoming constraints cap the width
private fun Modifier.imageSize(alt: ImageAlt, pictureSize: Size): Modifier {
    val width = (alt.width ?: pictureSize.takeIf { it.hasArea() }?.width?.roundToInt())?.toImageSide()
        ?: return this
    val height = alt.height?.toImageSide()

    return when {
        height != null -> size(width, height)
        // when only width is provided, the image scales up with locked aspect ratio
        pictureSize.hasArea() -> width(width).aspectRatio(pictureSize.width / pictureSize.height)
        else -> width(width)
    }
}

private fun Size.hasArea(): Boolean = isSpecified && width > 0f && height > 0f

// --- the image node ------------------------------------------------------------------------------

// ![alt](link), or ![alt][reference] with the link defined elsewhere in the note
private fun ASTNode.imageLink(content: String, referenceLinkHandler: ReferenceLinkHandler): String? {
    findDescendant(MarkdownElementTypes.LINK_DESTINATION)?.let { return it.getUnescapedTextInNode(content) }

    val reference = findDescendant(MarkdownElementTypes.FULL_REFERENCE_LINK)
        ?: findDescendant(MarkdownElementTypes.SHORT_REFERENCE_LINK)
        ?: return null
    val label = reference.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }
        ?.getUnescapedTextInNode(content)
        ?: return null
    return referenceLinkHandler.find(label).takeIf { it.isNotEmpty() }
}

private fun ASTNode.imageAltText(content: String): String? =
    (findDescendant(MarkdownElementTypes.LINK_TEXT) ?: findDescendant(MarkdownElementTypes.LINK_LABEL))
        ?.getUnescapedTextInNode(content)
        ?.trim('[', ']')
