package dev.jotalac.feature.editor.ui.components.editor_find

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.IconButton
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.theme.AppTheme
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MarkdownFindBar(
    findState: MarkdownFindState,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()

    }

    Row(
        modifier = modifier
            .clip(AppTheme.shapes.medium)
            .background(AppTheme.colors.surfaceSunken)
            .border(1.dp, AppTheme.colors.border, AppTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = findState.query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = AppTheme.typography.body.copy(color = AppTheme.colors.text),
            cursorBrush = SolidColor(AppTheme.colors.accent),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onNext() }),
            decorationBox = { innerTextField ->
                Box {
                    if (findState.query.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.find_placeholder),
                            style = AppTheme.typography.body,
                            color = AppTheme.colors.textSecondary.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                }
            },
        )

        Text(
            text = if (findState.matches.isEmpty()) "0/0"
            else "${findState.currentIndex + 1}/${findState.matches.size}",
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )

        IconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(Res.drawable.arrow_up_filled),
                contentDescription = stringResource(Res.string.find_previous),
                tint = AppTheme.colors.textSecondary,
            )
        }

        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(Res.drawable.arrow_down_filled),
                contentDescription = stringResource(Res.string.find_next),
                tint = AppTheme.colors.textSecondary,
            )
        }

        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(Res.drawable.x_icon),
                contentDescription = stringResource(Res.string.close),
                tint = AppTheme.colors.textSecondary,
            )
        }
    }
}
