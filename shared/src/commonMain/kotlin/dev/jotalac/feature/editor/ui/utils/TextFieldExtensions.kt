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

fun TextFieldValue.applyBold(): TextFieldValue = this.applyMarkdownSyntax("**", "**")

fun TextFieldValue.applyH1(): TextFieldValue = this.applyMarkdownSyntax("# ")

fun TextFieldValue.applyH2(): TextFieldValue = this.applyMarkdownSyntax("## ")

fun TextFieldValue.applyH3(): TextFieldValue = this.applyMarkdownSyntax("### ")

fun TextFieldValue.applyItalic(): TextFieldValue = this.applyMarkdownSyntax("*", "*")

fun TextFieldValue.applyBulletedList(): TextFieldValue = this.applyMarkdownSyntax("- ")

fun TextFieldValue.applyNumberedList(): TextFieldValue = this.applyMarkdownSyntax("1. ")

fun TextFieldValue.applyQuote(): TextFieldValue = this.applyMarkdownSyntax("> ")

fun TextFieldValue.applyCodeBlock(): TextFieldValue = this.applyMarkdownSyntax("```\n", "\n```")

fun TextFieldValue.applyInlineCode(): TextFieldValue = this.applyMarkdownSyntax("`", "`")

fun TextFieldValue.applyCheckbox(): TextFieldValue = this.applyMarkdownSyntax("- [ ]")

fun TextFieldValue.addLink(): TextFieldValue = this.applyMarkdownSyntax("[", "]()")

fun TextFieldValue.addMathBlock(): TextFieldValue = this.applyMarkdownSyntax("$$", "$$")

fun TextFieldValue.increaseIndentation(): TextFieldValue = handleIndentation(this, isUntab = false)

fun TextFieldValue.decreaseIndentation(): TextFieldValue = handleIndentation(this, isUntab = true)

fun TextFieldValue.handleNewLine(): TextFieldValue = handleNewLineWithinBlock(this)

