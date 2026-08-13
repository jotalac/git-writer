package dev.jotalac.feature.editor.ui.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun TextFieldValue.applyMarkdownSyntax(prefix: String = "", suffix: String = ""): TextFieldValue {
    val selStart = selection.start.coerceAtMost(selection.end)
    val selEnd = selection.end.coerceAtLeast(selection.start)


    val textBefore = text.substring(0, selStart)
    val selectedText = text.substring(selStart, selEnd)
    val textAfter = text.substring(selEnd)

    // create the new text with the special characters
    val newText = "$textBefore$prefix$selectedText$suffix$textAfter"

    // calculate the cursor positions
    val newCursorPos = if (selectedText.isEmpty()) {
        selection.start + prefix.length
    } else {
        selection.start + prefix.length + selectedText.length + suffix.length
    }

    return TextFieldValue(text = newText, selection = TextRange(newCursorPos))
}