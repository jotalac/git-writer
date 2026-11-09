package dev.jotalac.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.IconButton
import dev.jotalac.core.ui.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun TopAppBarIcon(
    onClick: () -> Unit,
    icon: DrawableResource,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(AppTheme.dimensions.iconLarge)
        )
    }
}