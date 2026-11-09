package dev.jotalac.feature.editor.ui.live_preview

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode

/**
 * Drops the requests of the content inside it to be scrolled into view
 */
internal fun Modifier.dropScrollRequests(): Modifier = this then DropScrollRequestsElement

private data object DropScrollRequestsElement : ModifierNodeElement<DropScrollRequestsNode>() {
    override fun create() = DropScrollRequestsNode()

    override fun update(node: DropScrollRequestsNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "dropScrollRequests"
    }
}

private class DropScrollRequestsNode : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?) = Unit
}
