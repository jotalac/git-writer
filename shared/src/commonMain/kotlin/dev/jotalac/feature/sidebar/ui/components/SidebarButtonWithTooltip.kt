package dev.jotalac.feature.sidebar.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.jotalac.core.ui.components.DelayedTooltip
import dev.jotalac.core.ui.theme.dimensions
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SidebarButtonWithTooltip(
    onClick: () -> Unit,
    icon: DrawableResource,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    enabled: Boolean = true,
) {
    DelayedTooltip(
        text = contentDescription,
        delayTime = 500.milliseconds
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