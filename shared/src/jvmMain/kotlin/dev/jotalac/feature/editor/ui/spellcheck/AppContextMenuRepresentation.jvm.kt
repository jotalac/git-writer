package dev.jotalac.feature.editor.ui.spellcheck

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.DesktopContextMenu
import dev.jotalac.core.ui.theme.dimensions

/**
 * Renders the editor's context menu with the app's own chrome instead of Compose's.
 *
 * Two reasons this exists rather than using the bundled representations:
 *
 * 1. `LocalContextMenuRepresentation` defaults to [androidx.compose.foundation.LightDefaultContextMenuRepresentation],
 *    which is hardcoded to a white background and a black foreground, so the menu ignored this
 *    app's dark theme.
 * 2. Compose's own representation throws "layouts are not part of the same hierarchy" from
 *    `NodeCoordinator.findCommonAncestor` when an item is clicked — for Cut/Copy/Paste too, so it
 *    is the menu chrome and not the action being run.
 *
 * [DesktopContextMenu] is the same `DropdownMenu` the sidebar menus use, so it anchors inside the
 * field's own subtree and never converts between hierarchies. Rendering the rows here also allows
 * real dividers and the compact desktop item height instead of Material's 48dp menu rows.
 *
 * @param spellcheckItemCount how many trailing items this app contributes, so the renderer knows
 *   where its block starts and whether there are suggestions to separate from "Add to dictionary".
 */
internal class AppContextMenuRepresentation(
    private val spellcheckItemCount: () -> Int,
) : ContextMenuRepresentation {

    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status as? ContextMenuState.Status.Open ?: return
        val allItems = items()
        if (allItems.isEmpty()) return

        val ownCount = spellcheckItemCount()
        // our items are appended last, so the block starts here
        val blockStart = (allItems.size - ownCount).coerceIn(0, allItems.size)
        val close = { state.status = ContextMenuState.Status.Closed }

        // status.rect is already relative to the anchor, which is the box this menu is composed
        // in, so it maps straight onto DropdownMenu's anchor-relative offset.
        val density = LocalDensity.current
        val offset = with(density) {
            DpOffset(x = status.rect.left.toDp(), y = status.rect.top.toDp())
        }

        DesktopContextMenu(
            expanded = true,
            onDismissRequest = close,
            offset = offset,
        ) {
            Column {
                allItems.forEachIndexed { index, item ->
                    val dividerBefore =
                        (index == blockStart && index != 0) ||
                            (ownCount > 2 && index == allItems.lastIndex)

                    if (dividerBefore) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }

                    MenuRow(
                        label = item.label,
                        enabled = item.enabled,
                        onClick = {
                            close()
                            item.onClick()
                        },
                    )
                }
            }
        }
    }
}

/** One compact menu row, matching the height and text size of the sidebar menus. */
@Composable
private fun MenuRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .height(MaterialTheme.dimensions.contextMenuItemHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (hovered && enabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.dimensions.listItemTextSize
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
