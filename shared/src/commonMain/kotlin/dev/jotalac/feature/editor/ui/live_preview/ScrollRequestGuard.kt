package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.relocation.bringIntoView

/**
 * Drops the requests of the content to be scrolled into view while [isBlocked] - the others go on to the scrolling
 * parent as they are.
 */
internal fun Modifier.scrollRequestGuard(isBlocked: () -> Boolean): Modifier = this then ScrollRequestGuardElement(isBlocked)

private data class ScrollRequestGuardElement(val isBlocked: () -> Boolean) : ModifierNodeElement<ScrollRequestGuardNode>() {
    override fun create() = ScrollRequestGuardNode(isBlocked)

    override fun update(node: ScrollRequestGuardNode) {
        node.isBlocked = isBlocked
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollRequestGuard"
    }
}

private class ScrollRequestGuardNode(var isBlocked: () -> Boolean) : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?) {
        if (isBlocked()) return
        val coordinates = requireLayoutCoordinates()
        // asked again from here, in this node's coordinates
        (this as DelegatableNode).bringIntoView {
            if (!childCoordinates.isAttached || !coordinates.isAttached) return@bringIntoView null
            boundsProvider()?.translate(coordinates.localPositionOf(childCoordinates, Offset.Zero))
        }
    }
}
