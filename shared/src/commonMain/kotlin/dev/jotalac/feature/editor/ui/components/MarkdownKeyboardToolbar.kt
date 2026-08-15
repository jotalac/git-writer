package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.editor.ui.EditorAction
import dev.jotalac.feature.editor.ui.utils.*
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MarkdownKeyboardToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onImageAdd: () -> Unit,
    onCameraOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToolbarButton(Res.drawable.format_h1) {
            onValueChange(textFieldValue.applyH1())
        }

        ToolbarButton(Res.drawable.format_h2) {
            onValueChange(textFieldValue.applyH2())
        }

        ToolbarButton(Res.drawable.format_h3) {
            onValueChange(textFieldValue.applyH3())
        }

        ToolbarButton(Res.drawable.format_bold) {
            onValueChange(textFieldValue.applyBold())
        }

        ToolbarButton(Res.drawable.format_italic) {
            onValueChange(textFieldValue.applyItalic())
        }

        ToolbarButton(Res.drawable.attach_file_add) {
            onImageAdd()
        }

        ToolbarButton(Res.drawable.photo_camera) {
            onCameraOpen()
        }

        ToolbarButton(Res.drawable.format_list_bulleted) {
            onValueChange(textFieldValue.applyBulletedList())
        }

        ToolbarButton(Res.drawable.format_list_numbered) {
            onValueChange(textFieldValue.applyNumberedList())
        }

        ToolbarButton(Res.drawable.format_indent_increase) {
            onValueChange(textFieldValue.increaseIndentation())
        }

        ToolbarButton(Res.drawable.format_indent_decrease) {
            onValueChange(textFieldValue.decreaseIndentation())
        }

        ToolbarButton(Res.drawable.format_quote) {
            onValueChange(textFieldValue.applyQuote())
        }

        ToolbarButton(Res.drawable.format_code_block) {
            onValueChange(textFieldValue.applyCodeBlock())
        }

        ToolbarButton(Res.drawable.format_inline_code) {
            onValueChange(textFieldValue.applyInlineCode())
        }

        ToolbarButton(Res.drawable.format_checkbox) {
            onValueChange(textFieldValue.applyCheckbox())
        }

        ToolbarButton(Res.drawable.link) {
            onValueChange(textFieldValue.addLink())
        }
    }
}

@Composable
private fun ToolbarButton(icon: DrawableResource = Res.drawable.x_icon, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = "format option",
        )
    }
}