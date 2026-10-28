package dev.jotalac.feature.pdf_export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import dev.jotalac.core.domain.AppFontFamily
import dev.jotalac.core.domain.AppThemeAccentColor
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.encodeToJpeg
import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks
import dev.jotalac.feature.editor.ui.components.markdown_rendering.RenderedMarkdownBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// a page image: A4 at about 190 DPI, the same on every device
private val PageSize = IntSize(1588, 2246)
private const val PAGE_MARGIN_PX = 96

// how big the note is on paper compared to the app - text, spacing and images shrink together, the page keeps
// its resolution. 1 would make the body text 12 pt, 0.8 makes it about 9.5 pt
private const val CONTENT_SCALE = 0.85f
private val PageDensity = Density(density = 2f * CONTENT_SCALE, fontScale = 1f)

// how long images may take to load - a slow web image must not keep the export waiting forever
private val ImageLoadTimeout = 10.seconds

/**
 * Renders [markdown] like the editor does, in the light theme with the user's [accentColor] and [fontFamily],
 * and captures it as A4 page images for the PDF export. Laid out but never drawn on screen: the note is
 * recorded into a graphics layer, and every page is cut out of it. [onPagesRendered] gets the pages, or why
 * rendering failed.
 */
@Composable
fun NotePdfRenderer(
    markdown: String,
    accentColor: AppThemeAccentColor = AppThemeAccentColor.DEFAULT,
    fontFamily: AppFontFamily = AppFontFamily.DEFAULT,
    onPagesRendered: (Result<List<JpegPage>>) -> Unit,
) {
    val blocks = remember(markdown) { chunkMarkdownIntoBlocks(markdown).filter { it.isNotBlank() } }
    val blockHeights = remember(blocks) { mutableStateListOf<Int>().apply { repeat(blocks.size) { add(0) } } }
    val contentLayer = rememberGraphicsLayer()
    val graphicsContext = LocalGraphicsContext.current
    val platformContext = LocalPlatformContext.current
    val currentOnPagesRendered by rememberUpdatedState(onPagesRendered)
    var imagesLoaded by remember(markdown) { mutableStateOf(false) }

    LaunchedEffect(markdown) {
        // the images first, so the note is laid out with their real size from the start - those that take too
        // long are laid out as they come
        val imageLoader = SingletonImageLoader.get(platformContext)
        withTimeoutOrNull(ImageLoadTimeout) {
            imageLinks(markdown).forEach { link ->
                imageLoader.execute(ImageRequest.Builder(platformContext).data(link).build())
            }
        }
        imagesLoaded = true

        val result = suspendRunCatching {
            awaitStableLayout(blockHeights)

            paginate(blockHeights.toList(), pageHeight = PageSize.height - 2 * PAGE_MARGIN_PX).map { slice ->
                val layer = graphicsContext.createGraphicsLayer()
                try {
                    layer.recordPage(contentLayer, slice, PageSize, PAGE_MARGIN_PX)
                    val bitmap = layer.toImageBitmap()
                    val jpeg = withContext(Dispatchers.Default) { bitmap.encodeToJpeg() }
                    JpegPage(jpeg, bitmap.width, bitmap.height)
                } finally {
                    graphicsContext.releaseGraphicsLayer(layer)
                }
            }
        }
        currentOnPagesRendered(result)
    }

    if (!imagesLoaded) return

    // no space in the layout and nothing drawn on screen - the content is only recorded into the layer
    Box(Modifier.size(0.dp).wrapContentSize(Alignment.TopStart, unbounded = true)) {
        CompositionLocalProvider(LocalDensity provides PageDensity) {
            AppTheme(darkTheme = false, dynamicColor = false, accentColor = accentColor, fontFamily = fontFamily) {
                Column(
                    Modifier
                        .requiredWidth(with(PageDensity) { (PageSize.width - 2 * PAGE_MARGIN_PX).toDp() })
                        .drawWithContent { contentLayer.record { this@drawWithContent.drawContent() } }
                ) {
                    blocks.forEachIndexed { index, block ->
                        Box(
                            Modifier
                                // measured with the spacing around the block - pages are cut by these heights
                                .onSizeChanged { blockHeights[index] = it.height }
                                .padding(vertical = 8.dp)
                        ) {
                            RenderedMarkdownBlock(text = block, darkCodeTheme = false)
                        }
                    }
                }
            }
        }
    }
}

/** Waits until the blocks keep their size (images and fonts loaded) and the layer holds what they draw. */
@OptIn(FlowPreview::class)
private suspend fun awaitStableLayout(blockHeights: List<Int>) {
    withTimeoutOrNull(10.seconds) {
        snapshotFlow { blockHeights.toList() }.debounce(500.milliseconds).first()
    }
    // a frame more, so the layer is recorded with the final layout
    withFrameNanos { }
    withFrameNanos { }
}

/** Draws the part [slice] of the recorded note onto a white page, inside the margins. */
private fun GraphicsLayer.recordPage(content: GraphicsLayer, slice: PageSlice, pageSize: IntSize, margin: Int) {
    record(PageDensity, LayoutDirection.Ltr, pageSize) {
        drawRect(Color.White)
        val contentWidth = (pageSize.width - 2 * margin).toFloat()
        val sliceHeight = (slice.bottom - slice.top).toFloat()
        translate(margin.toFloat(), margin.toFloat()) {
            clipRect(0f, 0f, contentWidth, sliceHeight) {
                translate(0f, -slice.top.toFloat()) { drawLayer(content) }
            }
        }
    }
}

// `![alt](link)` - the link either in angle brackets (it may contain spaces then) or up to the first space
private val ImageLinkRegex = Regex("""!\[[^\]]*]\(\s*(?:<([^>]+)>|([^)\s]+))""")

/** The image links in [markdown], also of sized images `![alt|300](link)`. */
internal fun imageLinks(markdown: String): List<String> =
    ImageLinkRegex.findAll(markdown).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }.distinct().toList()
