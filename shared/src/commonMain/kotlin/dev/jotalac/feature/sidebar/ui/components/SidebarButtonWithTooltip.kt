package dev.jotalac.feature.sidebar.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.IconButton
import dev.jotalac.core.ui.design.Tooltip
import dev.jotalac.core.ui.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SidebarButtonWithTooltip(
    onClick: () -> Unit,
    icon: DrawableResource,
    contentDescription: String,
    tint: Color = AppTheme.colors.onAccentSoft,
    enabled: Boolean = true,
) {
    Tooltip(
        text = contentDescription,
        delayTime = 500.milliseconds
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(AppTheme.dimensions.buttonCompact),
            enabled = enabled
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = if (enabled) tint else AppTheme.colors.textMuted,
                modifier = Modifier.size(AppTheme.dimensions.iconLarge)
            )
        }
    }
}