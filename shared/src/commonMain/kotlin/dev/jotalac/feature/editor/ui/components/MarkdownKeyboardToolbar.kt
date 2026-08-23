package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.editor.ui.utils.*
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownKeyboardToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onImageAdd: () -> Unit,
    onCameraOpen: () -> Unit,
    onUndoRedo: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // undo redo
        ToolbarButton(
            icon = Res.drawable.undo,
            contentDescription = Res.string.undo_button
        ) {
            onUndoRedo(true)
        }
        ToolbarButton(
            icon = Res.drawable.redo,
            contentDescription = Res.string.redo_button
        ) {
            onUndoRedo(false)
        }

        // markdown edit states
        ToolbarButton(
            icon = Res.drawable.format_h1,
            contentDescription = Res.string.toolbar_h1
        ) {
            onValueChange(textFieldValue.applyH1())
        }

        ToolbarButton(
            icon = Res.drawable.format_h2,
            contentDescription = Res.string.toolbar_h2
        ) {
            onValueChange(textFieldValue.applyH2())
        }

        ToolbarButton(
            icon = Res.drawable.format_h3,
            contentDescription = Res.string.toolbar_h3
        ) {
            onValueChange(textFieldValue.applyH3())
        }

        ToolbarButton(
            icon = Res.drawable.format_bold,
            contentDescription = Res.string.toolbar_bold
        ) {
            onValueChange(textFieldValue.applyBold())
        }

        ToolbarButton(
            icon = Res.drawable.format_italic,
            contentDescription = Res.string.toolbar_italic
        ) {
            onValueChange(textFieldValue.applyItalic())
        }

        ToolbarButton(
            icon = Res.drawable.attach_file_add,
            contentDescription = Res.string.toolbar_attach_file
        ) {
            onImageAdd()
        }

        ToolbarButton(
            icon = Res.drawable.photo_camera,
            contentDescription = Res.string.toolbar_camera
        ) {
            onCameraOpen()
        }

        ToolbarButton(
            icon = Res.drawable.format_list_bulleted,
            contentDescription = Res.string.toolbar_bulleted_list
        ) {
            onValueChange(textFieldValue.applyBulletedList())
        }

        ToolbarButton(
            icon = Res.drawable.format_list_numbered,
            contentDescription = Res.string.toolbar_numbered_list
        ) {
            onValueChange(textFieldValue.applyNumberedList())
        }

        ToolbarButton(
            icon = Res.drawable.format_checkbox,
            contentDescription = Res.string.toolbar_checkbox
        ) {
            onValueChange(textFieldValue.applyCheckbox())
        }

        ToolbarButton(
            icon = Res.drawable.format_indent_increase,
            contentDescription = Res.string.toolbar_indent_increase
        ) {
            onValueChange(textFieldValue.increaseIndentation())
        }

        ToolbarButton(
            icon = Res.drawable.format_indent_decrease,
            contentDescription = Res.string.toolbar_indent_decrease
        ) {
            onValueChange(textFieldValue.decreaseIndentation())
        }

        ToolbarButton(
            icon = Res.drawable.line_down,
            contentDescription = Res.string.toolbar_new_line
        ) {
            onValueChange(textFieldValue.handleNewLine())
        }

        ToolbarButton(
            icon = Res.drawable.format_quote,
            contentDescription = Res.string.toolbar_quote
        ) {
            onValueChange(textFieldValue.applyQuote())
        }

        ToolbarButton(
            icon = Res.drawable.format_code_block,
            contentDescription = Res.string.toolbar_code_block
        ) {
            onValueChange(textFieldValue.applyCodeBlock())
        }

        ToolbarButton(
            icon = Res.drawable.format_inline_code,
            contentDescription = Res.string.toolbar_inline_code
        ) {
            onValueChange(textFieldValue.applyInlineCode())
        }

        ToolbarButton(
            icon = Res.drawable.link,
            contentDescription = Res.string.toolbar_link
        ) {
            onValueChange(textFieldValue.addLink())
        }

        ToolbarButton(
            icon = Res.drawable.math_function,
            contentDescription = Res.string.toolbar_math
        ) {
            onValueChange(textFieldValue.addMathBlock())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarButton(
    icon: DrawableResource = Res.drawable.x_icon,
    contentDescription: StringResource,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
            10.dp
        ),
        tooltip = {
            PlainTooltip { Text(stringResource(contentDescription)) }
        },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = stringResource(contentDescription),
            )
        }
    }
}