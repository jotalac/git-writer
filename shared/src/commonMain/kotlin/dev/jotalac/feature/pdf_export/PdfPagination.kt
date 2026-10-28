package dev.jotalac.feature.pdf_export

/** The part of the rendered note one PDF page shows, from [top] to [bottom] in pixels of the rendered note. */
data class PageSlice(val top: Int, val bottom: Int)

/**
 * Splits a note rendered as blocks of [blockHeights] into pages [pageHeight] tall. A page ends before a block
 * that does not fit on it anymore, so blocks are not cut - only a block taller than a whole page is, at the
 * page edge. An empty note still gives one (empty) page.
 */
fun paginate(blockHeights: List<Int>, pageHeight: Int): List<PageSlice> {
    require(pageHeight > 0) { "the page needs a height" }

    val pages = mutableListOf<PageSlice>()
    var pageTop = 0
    var blockTop = 0

    for (height in blockHeights) {
        val blockBottom = blockTop + height

        // the block does not fit anymore - it starts the next page, unless it is the first one on this page or
        // so tall it is cut anyway (then it fills the rest of this page)
        if (blockBottom - pageTop > pageHeight && blockTop > pageTop && height <= pageHeight) {
            pages += PageSlice(pageTop, blockTop)
            pageTop = blockTop
        }
        // taller than a page - cut it into whole pages, the rest shares a page with the following blocks
        while (blockBottom - pageTop > pageHeight) {
            pages += PageSlice(pageTop, pageTop + pageHeight)
            pageTop += pageHeight
        }

        blockTop = blockBottom
    }

    pages += PageSlice(pageTop, blockTop)
    return pages
}
