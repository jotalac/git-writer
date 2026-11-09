package dev.jotalac.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.MenuDivider
import dev.jotalac.core.ui.design.MenuItem
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** An item of a [DesktopContextMenu] - clicking it closes the menu first. */
@Composable
fun ContextMenuItem(
    text: String,
    onClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    iconPainter: DrawableResource? = null,
    itemColor: Color? = null,
    enabled: Boolean = true
) {
    MenuItem(
        text = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        onClick = {
            onDismissRequest()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        contentColor = itemColor ?: AppTheme.colors.text,
        leadingIcon = iconPainter?.let { icon ->
            {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(AppTheme.dimensions.iconMedium),
                )
            }
        },
    )
}

/** A title above a context menu's items, saying what they do - not clickable. */
@Composable
fun ContextMenuHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = text,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.textSecondary,
            // lined up with the text of the items below
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
        MenuDivider()
    }
}
