package dev.jotalac.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jotalac.core.ui.theme.dimensions
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// if the item is in a ModalBottomSheet
internal val LocalMenuInSheet = staticCompositionLocalOf { false }

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
    val inSheet = LocalMenuInSheet.current
    val shape = RoundedCornerShape(if (inSheet) 12.dp else 6.dp)

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (inSheet) 16.dp else 8.dp),
                modifier = modifier.clip(shape)
            ) {
                if (iconPainter != null) {
                    Icon(
                        painter = painterResource(iconPainter),
                        contentDescription = text,
                        modifier = Modifier
                            .size(MaterialTheme.dimensions.iconMedium),
                        tint = itemColor ?: if (inSheet) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text,
                    style = if (inSheet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium.copy(
                        fontSize = MaterialTheme.dimensions.listItemTextSize
                    ),
                    fontWeight = if (inSheet) FontWeight.Normal else FontWeight.Medium,
                    color = itemColor ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        onClick = {
            onDismissRequest()
            onClick()
        },
        // a disabled item cannot be clicked or hovered
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = if (inSheet) 16.dp else 8.dp, vertical = 0.dp),
        modifier = modifier
            .height(MaterialTheme.dimensions.contextMenuItemHeight)
            .clip(shape)
            .padding(3.dp)
    )
}

/** A title above a context menu's items, saying what they do - not clickable. */
@Composable
fun ContextMenuHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    // in a sheet a quiet label over its group, like the sidebar's sections - in a menu with a line under it
    if (LocalMenuInSheet.current) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(start = 19.dp, end = 19.dp, top = 12.dp, bottom = 4.dp),
        )
    } else {
        Column(modifier = modifier) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // lined up with the text of the items below
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 4.dp),
            )
        }
    }

}
