package dev.jotalac.feature.editor.ui.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Wraps the selection in [prefix] and [suffix] (bold, italic, inline code...), or unwraps it when it is already
 * wrapped - with the markers right around the selection ("**«bold»**") or selected with it ("«**bold**»"). The
 * text stays selected, so pressing the same button again undoes it. Without a selection the cursor ends up
 * between the markers.
 */
fun TextFieldValue.toggleMarkdownSyntax(prefix: String = "", suffix: String = ""): TextFieldValue {
    var start = selection.min
    var end = selection.max

    // markers selected together with the text count as around it
    val innerStart = start + prefix.length
    val innerEnd = end - suffix.length
    if (innerStart <= innerEnd && isWrapped(text.substring(0, innerStart), text.substring(innerEnd), prefix, suffix)) {
        start = innerStart
        end = innerEnd
    }

    return if (isWrapped(text.substring(0, start), text.substring(end), prefix, suffix)) {
        val unwrapped = text.removeRange(end, end + suffix.length).removeRange(start - prefix.length, start)
        TextFieldValue(unwrapped, TextRange(start - prefix.length, end - prefix.length))
    } else {
        val wrapped = text.substring(0, start) + prefix + text.substring(start, end) + suffix + text.substring(end)
        TextFieldValue(wrapped, TextRange(start + prefix.length, end + prefix.length))
    }
}

/** Whether [before] ends with [prefix] and [after] starts with [suffix] - the text on both sides of the selection. */
private fun isWrapped(before: String, after: String, prefix: String, suffix: String): Boolean {
    if (prefix.isEmpty() && suffix.isEmpty()) return false
    if (!before.endsWith(prefix) || !after.startsWith(suffix)) return false

    // italic "*" and bold "**" are the same character, so the whole run of it decides: "**bold**" has no italic
    // in it, "***both***" has both
    if (prefix == suffix && prefix.all { it == '*' }) {
        val runs = listOf(before.takeLastWhile { it == '*' }.length, after.takeWhile { it == '*' }.length)
        return runs.all { it == prefix.length || it == 3 }
    }
    return true
}

/**
 * Toggles a line prefix ("# ", "- ", "> "...) on the line with the cursor: it goes at the start of the line,
 * after its indentation, not at the cursor. A line that already starts with exactly this prefix loses it.
 * Only the prefix itself is checked, others stay: "## " on a "# " heading gives "## # heading".
 */
fun TextFieldValue.toggleMarkdownSyntaxBlock(prefix: String): TextFieldValue {
    val lineStart = text.lastIndexOf('\n', selection.min - 1) + 1
    var contentStart = lineStart
    while (contentStart < text.length && (text[contentStart] == ' ' || text[contentStart] == '\t')) contentStart++

    // the cursor and the selection move with the text after the prefix
    return if (text.startsWith(prefix, contentStart)) {
        val prefixEnd = contentStart + prefix.length
        fun shift(offset: Int) = when {
            offset >= prefixEnd -> offset - prefix.length
            offset > contentStart -> contentStart
            else -> offset
        }
        TextFieldValue(text.removeRange(contentStart, prefixEnd), TextRange(shift(selection.start), shift(selection.end)))
    } else {
        fun shift(offset: Int) = if (offset >= contentStart) offset + prefix.length else offset
        TextFieldValue(
            text.substring(0, contentStart) + prefix + text.substring(contentStart),
            TextRange(shift(selection.start), shift(selection.end)),
        )
    }
}

fun TextFieldValue.applyBold(): TextFieldValue = this.toggleMarkdownSyntax("**", "**")

fun TextFieldValue.applyH1(): TextFieldValue = this.toggleMarkdownSyntaxBlock("# ")

fun TextFieldValue.applyH2(): TextFieldValue = this.toggleMarkdownSyntaxBlock("## ")

fun TextFieldValue.applyH3(): TextFieldValue = this.toggleMarkdownSyntaxBlock("### ")

fun TextFieldValue.applyItalic(): TextFieldValue = this.toggleMarkdownSyntax("*", "*")

fun TextFieldValue.applyBulletedList(): TextFieldValue = this.toggleMarkdownSyntaxBlock("- ")

fun TextFieldValue.applyNumberedList(): TextFieldValue = this.toggleMarkdownSyntaxBlock("1. ")

fun TextFieldValue.applyQuote(): TextFieldValue = this.toggleMarkdownSyntaxBlock("> ")

fun TextFieldValue.applyCodeBlock(): TextFieldValue = this.toggleMarkdownSyntax("```\n", "\n```")

fun TextFieldValue.applyInlineCode(): TextFieldValue = this.toggleMarkdownSyntax("`", "`")

fun TextFieldValue.applyCheckbox(): TextFieldValue = this.toggleMarkdownSyntaxBlock("- [ ] ")

fun TextFieldValue.addLink(): TextFieldValue = this.toggleMarkdownSyntax("[", "]()")

fun TextFieldValue.addMathBlock(): TextFieldValue = this.toggleMarkdownSyntax("$$", "$$")

fun TextFieldValue.increaseIndentation(): TextFieldValue = handleIndentation(this, isUntab = false)

fun TextFieldValue.decreaseIndentation(): TextFieldValue = handleIndentation(this, isUntab = true)

fun TextFieldValue.handleNewLine(): TextFieldValue = handleNewLineWithinBlock(this)

