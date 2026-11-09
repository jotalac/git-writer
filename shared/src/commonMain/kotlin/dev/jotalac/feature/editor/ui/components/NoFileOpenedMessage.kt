package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.isDesktopPlatform
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoFileOpenedMessage() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .background(AppTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.info_icon),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppTheme.colors.textSecondary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.no_file_opened_title),
                style = AppTheme.typography.heading,
                color = AppTheme.colors.textSecondary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.no_file_opened_subtitle),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )

            // display the shortcuts for desktop
            if (isDesktopPlatform) {
                Spacer(modifier = Modifier.height(24.dp))
                KeyboardShortcutInfo(stringResource(Res.string.open_new_file_message), listOf("ctrl/⌘", "n"))
            }
        }
    }
}

@Composable
private fun KeyboardShortcutInfo(
    shortcutDescription: String,
    shortcutKeys: List<String>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            shortcutDescription,
            style = AppTheme.typography.heading,
            color = AppTheme.colors.textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 20.dp)
        )

        shortcutKeys.forEach { shortcutKey ->
            Text(
                shortcutKey,
                style = AppTheme.typography.heading,
                color = AppTheme.colors.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(AppTheme.shapes.medium)
                    .background(AppTheme.colors.surface)
                    .padding(6.dp)
            )

            if (shortcutKey != shortcutKeys.last())
                Text(
                    "+",
                    style = AppTheme.typography.heading,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.6f),
                )
        }
    }
}