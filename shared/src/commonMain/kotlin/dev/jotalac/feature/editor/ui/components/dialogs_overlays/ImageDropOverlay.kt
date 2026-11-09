package dev.jotalac.feature.editor.ui.components.dialogs_overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.design.Surface
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.theme.AppTheme

@Composable
fun ImageDropOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background.copy(alpha = 0.75f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = AppTheme.shapes.large,
            color = AppTheme.colors.accentSoft.copy(alpha = 0.5f),
            contentColor = AppTheme.colors.onAccentSoft,
            modifier = Modifier
                .fillMaxSize()
                .border(width = 2.dp, color = AppTheme.colors.accent, shape = AppTheme.shapes.large)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Drop image here to add to note",
                    style = AppTheme.typography.title
                )
            }
        }
    }
}
