package dev.jotalac.feature.editor.ui.spellcheck

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.DesktopContextMenu
import dev.jotalac.core.ui.theme.dimensions
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Marker for a separator travelling through Compose's context menu item list.
 *
 * [ContextMenuItem] has no separator type, and the renderer that could draw one
 * (`DefaultOpenContextMenu`) is internal, so a subclass gives the renderer something type-safe to
 * recognise. Nothing else consumes these items.
 */
internal class SeparatorMenuItem : ContextMenuItem(label = "", enabled = false, onClick = {})

/**
 * Renders the editor's context menu with the app's own chrome instead of Compose's bundled
 * representations.
 */
internal class AppContextMenuRepresentation : ContextMenuRepresentation {

    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status as? ContextMenuState.Status.Open ?: return
        val menuItems = items()
        if (menuItems.isEmpty()) return

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
                menuItems.forEach { item ->
                    if (item is SeparatorMenuItem) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    } else {
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
}

/**
 * One compact menu row, matching the height and text size of the sidebar menus.
 *
 * Built from Material 3's [DropdownMenuItem] rather than a hand-rolled clickable box: that is the
 * row the sidebar menus use, and the one arrow-key navigation works with.
 */
@Composable
private fun MenuRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: DrawableResource? = null
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.dimensions.listItemTextSize
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(MaterialTheme.dimensions.contextMenuItemHeight),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        leadingIcon = icon?.let { resource ->
            {
                Icon(
                    painter = painterResource(resource),
                    contentDescription = null,
                )
            }
        },
    )
}
