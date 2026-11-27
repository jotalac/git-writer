package dev.jotalac.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/*
* A long press on the file tree's background opens its menu on a phone - a long press on one of its rows is the row's
* (it drags the row), and a short tap is no long press.
* */
@OptIn(InternalComposeUiApi::class)
class LongPressTest {

    private var longPressedAt: DpOffset? = null
    private var rowClicks = 0

    // a row 50 dp tall at the top of a background that takes long presses
    private val scene = ImageComposeScene(width = 300, height = 300, density = Density(1f)) {
        Box(Modifier.fillMaxSize().onLongPress { longPressedAt = it }) {
            Box(Modifier.fillMaxWidth().height(50.dp).clickable { rowClicks++ })
        }
    }
    private var time = 0L

    @AfterTest
    fun close() = scene.close()

    private fun frames(millis: Long) {
        val end = time + millis * 1_000_000
        while (time < end) {
            scene.render(time)
            time += 16_000_000
            Thread.sleep(16)
        }
    }

    private fun press(at: Offset, holdMillis: Long) {
        frames(50)
        scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Touch)
        frames(holdMillis)
        scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Touch)
        frames(50)
    }

    @Test
    fun `a long press on the background opens the menu where it was`() {
        press(Offset(100f, 200f), holdMillis = 800)

        assertEquals(DpOffset(100.dp, 200.dp), longPressedAt)
    }

    @Test
    fun `a long press on a row is the row's`() {
        press(Offset(100f, 20f), holdMillis = 800)

        assertNull(longPressedAt)
    }

    @Test
    fun `a tap is no long press`() {
        press(Offset(100f, 200f), holdMillis = 50)
        press(Offset(100f, 20f), holdMillis = 50)

        assertNull(longPressedAt)
        assertEquals(1, rowClicks)
    }
}
