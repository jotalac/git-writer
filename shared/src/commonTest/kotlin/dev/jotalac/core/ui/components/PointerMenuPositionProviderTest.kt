package dev.jotalac.core.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class PointerMenuPositionProviderTest {

    private val window = IntSize(1000, 800)
    private val menu = IntSize(200, 300)

    // an anchor as tall as an editor, scrolled so its top is above the window
    private val tallAnchor = IntRect(left = 300, top = -2000, right = 900, bottom = 3000)

    private fun position(point: IntOffset, anchor: IntRect = tallAnchor, menuSize: IntSize = menu) =
        PointerMenuPositionProvider(point, margin = 8)
            .calculatePosition(anchor, window, LayoutDirection.Ltr, menuSize)

    @Test
    fun `the menu opens at the point, counted from the anchor's top - not its bottom`() {
        assertEquals(IntOffset(400, 100), position(IntOffset(100, 2100)))
    }

    @Test
    fun `a menu that does not fit below moves up just enough`() {
        // the point is at y 700 - the menu ends at the window's bottom margin instead of jumping elsewhere
        assertEquals(IntOffset(400, 800 - 8 - 300), position(IntOffset(100, 2700)))
    }

    @Test
    fun `a menu that does not fit on the right moves left just enough`() {
        assertEquals(IntOffset(1000 - 8 - 200, 100), position(IntOffset(650, 2100)))
    }

    @Test
    fun `a menu taller than the window starts at its top`() {
        assertEquals(8, position(IntOffset(100, 2100), menuSize = IntSize(200, 900)).y)
    }
}
