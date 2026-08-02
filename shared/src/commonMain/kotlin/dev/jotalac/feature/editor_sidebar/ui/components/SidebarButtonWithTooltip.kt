package dev.jotalac.feature.editor_sidebar.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.dimensions
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun SidebarButtonWithTooltip(
    onClick: () -> Unit,
    icon: DrawableResource,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    enabled: Boolean = true,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
            10.dp
        ),
        tooltip = {
            PlainTooltip { Text(contentDescription) }
        },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(MaterialTheme.dimensions.buttonCompact),
            enabled = enabled
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = if (enabled) tint else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(MaterialTheme.dimensions.iconLarge)
            )
        }
    }
}