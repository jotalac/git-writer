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
import dev.jotalac.feature.editor.ui.utils.applyMarkdownSyntax
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MarkdownKeyboardToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
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
            onValueChange(textFieldValue.applyMarkdownSyntax("# ", ""))
        }

        ToolbarButton(Res.drawable.format_h2) {
            onValueChange(textFieldValue.applyMarkdownSyntax("## ", ""))
        }

        ToolbarButton(Res.drawable.format_h3) {
            onValueChange(textFieldValue.applyMarkdownSyntax("### ", ""))
        }

        ToolbarButton(Res.drawable.format_bold) {
            onValueChange(textFieldValue.applyMarkdownSyntax("**", "**"))
        }

        ToolbarButton(Res.drawable.format_italic) {
            onValueChange(textFieldValue.applyMarkdownSyntax("*", "*"))
        }

        ToolbarButton(Res.drawable.format_quote) {
            onValueChange(textFieldValue.applyMarkdownSyntax("> ", ""))
        }

        ToolbarButton(Res.drawable.format_list_bulleted) {
            onValueChange(textFieldValue.applyMarkdownSyntax("- ", ""))
        }

        ToolbarButton(Res.drawable.format_list_numbered) {
            onValueChange(textFieldValue.applyMarkdownSyntax("1. ", ""))
        }

        ToolbarButton(Res.drawable.format_code_block) {
            onValueChange(textFieldValue.applyMarkdownSyntax("```\n", "\n```"))
        }

        ToolbarButton(Res.drawable.format_inline_code) {
            onValueChange(textFieldValue.applyMarkdownSyntax("`", "`"))
        }

        ToolbarButton(Res.drawable.format_checkbox) {
            onValueChange(textFieldValue.applyMarkdownSyntax("`", "`"))
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