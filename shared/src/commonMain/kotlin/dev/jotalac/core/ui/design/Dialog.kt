package dev.jotalac.core.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.jotalac.core.ui.theme.AppTheme
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.close
import git_writer.shared.generated.resources.x_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

enum class DialogSize {
    /** As wide as a form needs, as tall as its content. */
    Compact,

    /** Most of the window - the settings. */
    Large,
}

/**
 * A dialog card raised over a scrim - a [title] with an optional [icon], the [content], and the [actions] at the
 * bottom right. While not [dismissible], a click outside or the back key do not close it (a save in progress).
 *
 * The content scrolls when [scrollContent], otherwise it gets the room left and handles it itself (a list).
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: Painter? = null,
    size: DialogSize = DialogSize.Compact,
    dismissible: Boolean = true,
    showCloseButton: Boolean = false,
    scrollContent: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = appDialogProperties(dismissible = dismissible, fullScreen = false, scrim = colors.scrim),
    ) {
        Surface(
            shape = AppTheme.shapes.large,
            color = colors.surfaceRaised,
            border = colors.border,
            shadowElevation = 16.dp,
            modifier = modifier
                .padding(24.dp)
                .then(
                    when (size) {
                        DialogSize.Compact -> Modifier.widthIn(max = 520.dp).fillMaxWidth()
                        DialogSize.Large -> Modifier
                            .widthIn(min = 720.dp, max = 1100.dp)
                            .fillMaxWidth(0.85f)
                            .heightIn(min = 520.dp, max = 850.dp)
                            .fillMaxHeight(0.85f)
                    }
                ),
        ) {
            Column {
                if (title != null || showCloseButton) {
                    DialogHeader(title, icon, onClose = onDismissRequest.takeIf { showCloseButton && dismissible })
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = size == DialogSize.Large)
                        .then(if (scrollContent) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )

                if (actions != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                } else {
                    // the content does not end right at the card's edge
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String?, icon: Painter?, onClose: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = if (onClose != null) 10.dp else 20.dp, top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
        }
        Text(
            text = title.orEmpty(),
            style = AppTheme.typography.title,
            modifier = Modifier.weight(1f),
        )
        if (onClose != null) {
            IconButton(onClick = onClose, contentColor = AppTheme.colors.textSecondary) {
                Icon(
                    painter = painterResource(Res.drawable.x_icon),
                    contentDescription = stringResource(Res.string.close),
                    modifier = Modifier.size(AppTheme.dimensions.iconMedium),
                )
            }
        }
    }
}
