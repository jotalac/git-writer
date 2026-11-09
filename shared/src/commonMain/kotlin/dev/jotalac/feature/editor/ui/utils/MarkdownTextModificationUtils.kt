package dev.jotalac.feature.editor.ui.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue


fun isInsideCodeBlock(text: String, cursorIndex: Int): Boolean {
    val textBeforeCursor = text.substring(0, cursorIndex)
    val fenceCount = textBeforeCursor.split("```").size - 1
    return fenceCount % 2 != 0
}

val numberedListRegex = Regex("^(\\d+)\\. ")

fun insertTextBetween(
    text: String,
    leftSplitIndex: Int,
    rightSplitIndex: Int = leftSplitIndex,
    insertText: String = ""
): String {
    return text.substring(0, leftSplitIndex) + insertText + text.substring(rightSplitIndex)
}

fun handleNewLineWithinBlock(currentValue: TextFieldValue): TextFieldValue {
    val text = currentValue.text
    val cursorIndex = currentValue.selection.start
    val newText = insertTextBetween(text = text, leftSplitIndex = cursorIndex, insertText = "  \n")

    return TextFieldValue(text = newText, selection = TextRange(cursorIndex + 3))
}

fun exitListContinuation(lastLineIndex: Int, cursorIndex: Int, text: String): TextFieldValue {
    val lineStartIndex = lastLineIndex + 1

    val cleanText = insertTextBetween(text, lineStartIndex, cursorIndex)
    // the cursor stays on the line the list marker was removed from
    val newCursorPos = lineStartIndex

    return TextFieldValue(text = cleanText, selection = TextRange(newCursorPos))
}

fun handleUnorderedList(
    fullText: String,
    currentLineText: String,
    lastLineIndex: Int,
    cursorIndex: Int,
    startSpaces: Int
): TextFieldValue? {
    if (currentLineText == "- ") {
        return exitListContinuation(lastLineIndex, cursorIndex, fullText)
    } else if (currentLineText.startsWith("- ")) {
        val insertText = "\n" + " ".repeat(startSpaces) + "- "
        val newText = insertTextBetween(text = fullText, leftSplitIndex = cursorIndex, insertText = insertText)

        val newCursor = TextRange(cursorIndex + insertText.length)
        return TextFieldValue(text = newText, selection = newCursor)
    }

    return null
}

fun handleCheckboxes(
    fullText: String,
    currentLineText: String,
    lastLineIndex: Int,
    cursorIndex: Int,
    startSpaces: Int
): TextFieldValue? {
    if (currentLineText in listOf("- [ ] ", "- [x] ")) {
        return exitListContinuation(lastLineIndex, cursorIndex, fullText)
    } else if (currentLineText.startsWith("- [ ] ") || currentLineText.startsWith("- [x] ")) {
        val insertText = "\n" + " ".repeat(startSpaces) + "- [ ] "
        val newText = insertTextBetween(text = fullText, leftSplitIndex = cursorIndex, insertText = insertText)

        val newCursor = TextRange(cursorIndex + insertText.length)
        return TextFieldValue(text = newText, selection = newCursor)
    }

    return null
}

fun handleOrderedList(
    fullText: String,
    currentLineText: String,
    lastLineIndex: Int,
    cursorIndex: Int,
    spacesStart: Int
): TextFieldValue? {
    val numberMatch = numberedListRegex.find(currentLineText)

    if (numberMatch != null && currentLineText == numberMatch.value) {
        return exitListContinuation(lastLineIndex, cursorIndex, fullText)

    } else if (numberMatch != null) {
        val insertText = "\n" + " ".repeat(spacesStart) + "1. "

        val newText = insertTextBetween(text = fullText, leftSplitIndex = cursorIndex, insertText = insertText)
        val newCursorPos = cursorIndex + insertText.length

        return TextFieldValue(text = newText, selection = TextRange(newCursorPos))
    }

    return null
}

fun handleBlockQuotes(
    fullText: String,
    currentLineText: String,
    lastLineIndex: Int,
    cursorIndex: Int
): TextFieldValue? {
    if (currentLineText.trim() == ">") {
        return exitListContinuation(lastLineIndex, cursorIndex, fullText)
    } else if (currentLineText.startsWith(">")) {
        val insertText = "  \n> "
        val newText = insertTextBetween(text = fullText, leftSplitIndex = cursorIndex, insertText = insertText)

        val newCursor = TextRange(cursorIndex + insertText.length)
        return TextFieldValue(text = newText, selection = newCursor)
    }

    return null
}

fun handleMarkdownListContinuation(currentValue: TextFieldValue): TextFieldValue? {
    val text = currentValue.text
    val cursorIndex = currentValue.selection.start
    val textBeforeCursor = text.substring(0, cursorIndex)

    val lastLineIndex = textBeforeCursor.lastIndexOf('\n')
    val currentLineToCursor = textBeforeCursor.substring(lastLineIndex + 1)

    val spacesStart = currentLineToCursor.takeWhile { it == ' ' }.length
    val cleanCurrentLine = currentLineToCursor.trimStart()

    return handleCheckboxes(text, cleanCurrentLine, lastLineIndex, cursorIndex, spacesStart)
        ?: handleUnorderedList(text, cleanCurrentLine, lastLineIndex, cursorIndex, spacesStart)
        ?: handleOrderedList(text, cleanCurrentLine, lastLineIndex, cursorIndex, spacesStart)
        ?: handleBlockQuotes(text, cleanCurrentLine, lastLineIndex, cursorIndex)
}


/**
 * When [newValue] is [oldValue] with one line break typed at the cursor, [oldValue] with the list (or quote) it
 * was typed in continued on the new line - or null when it was some other edit, or no list.
 * Done on the text change, not on the Enter key, because a mobile keyboard types the line break as text.
 */
fun continueMarkdownOnNewLine(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue? {
    val cursor = newValue.selection.start
    if (!newValue.selection.collapsed || cursor < 1) return null
    if (newValue.text.length != oldValue.text.length + 1 || newValue.text[cursor - 1] != '\n') return null
    if (newValue.text.removeRange(cursor - 1, cursor) != oldValue.text) return null
    if (isInsideCodeBlock(oldValue.text, cursor - 1)) return null

    return handleMarkdownListContinuation(TextFieldValue(oldValue.text, TextRange(cursor - 1)))
}

/** Moves the lines with the cursor (or the selection) one line [up] or down - Alt+arrow. */
fun TextFieldValue.moveLines(up: Boolean): TextFieldValue {
    val start = text.lastIndexOf('\n', selection.min - 1) + 1
    val end = text.indexOf('\n', selection.max).let { if (it == -1) text.length else it }
    val lines = text.substring(start, end)

    return if (up) {
        if (start == 0) return this
        val previousStart = text.lastIndexOf('\n', start - 2) + 1
        val previous = text.substring(previousStart, start - 1)
        val newText = text.substring(0, previousStart) + lines + "\n" + previous + text.substring(end)
        val shift = start - previousStart
        TextFieldValue(newText, TextRange(selection.start - shift, selection.end - shift))
    } else {
        if (end == text.length) return this
        val nextEnd = text.indexOf('\n', end + 1).let { if (it == -1) text.length else it }
        val next = text.substring(end + 1, nextEnd)
        val newText = text.substring(0, start) + next + "\n" + lines + text.substring(nextEnd)
        val shift = next.length + 1
        TextFieldValue(newText, TextRange(selection.start + shift, selection.end + shift))
    }
}

/**
 * Puts [imageMarkdown] into the text after the line with the cursor (or in place of the empty line with it), each
 * image a paragraph of its own - so it is drawn as a picture - with the cursor after them.
 */
fun TextFieldValue.insertImageMarkdown(imageMarkdown: List<String>): TextFieldValue {
    if (imageMarkdown.isEmpty()) return this

    val lineStart = text.lastIndexOf('\n', selection.max - 1) + 1
    val lineEnd = text.indexOf('\n', selection.max).let { if (it == -1) text.length else it }
    val isLineEmpty = text.substring(lineStart, lineEnd).isBlank()

    val insertAt = if (isLineEmpty) lineStart else lineEnd
    val textBefore = text.substring(0, insertAt)
    // an empty line between the images and the text before them - one that is there already counts
    val before = when {
        textBefore.isEmpty() || textBefore.endsWith("\n\n") -> ""
        textBefore.endsWith("\n") -> "\n"
        else -> "\n\n"
    }
    val images = before + imageMarkdown.joinToString("\n\n") + "\n\n"
    val rest = text.substring(if (isLineEmpty) lineEnd else insertAt).removePrefix("\n")

    val newText = text.substring(0, insertAt) + images + rest
    return TextFieldValue(newText, TextRange(insertAt + images.length))
}

fun handleIndentation(currentValue: TextFieldValue, isUntab: Boolean): TextFieldValue {
    val text = currentValue.text
    val rawStart = currentValue.selection.start
    val rawEnd = currentValue.selection.end

    val minCursor = minOf(rawStart, rawEnd)
    val maxCursor = maxOf(rawStart, rawEnd)

    val startLineIndex = text.lastIndexOf('\n', minCursor - 1).coerceAtLeast(-1) + 1
    val endLineIndex = text.indexOf('\n', maxCursor).let { if (it == -1) text.length else it }

    val textBefore = text.substring(0, startLineIndex)
    val targetLines = text.substring(startLineIndex, endLineIndex)
    val textAfter = text.substring(endLineIndex)

    var firstLineShift = 0
    var totalShift = 0

    val modifiedLines = targetLines.split('\n').mapIndexed { index, line ->
        val shift: Int
        val newLine = if (isUntab) {
            // remove up to 4 leading spaces
            val leadingSpaces = line.takeWhile { it == ' ' }.length
            when {
                leadingSpaces > 0 -> {
                    val spacesToRemove = minOf(leadingSpaces, 4)
                    shift = -spacesToRemove
                    line.substring(spacesToRemove)
                }
                // fallback for tab signs
                line.startsWith("\t") -> {
                    shift = -1
                    line.substring(1)
                }
                // no indentation found
                else -> {
                    shift = 0
                    line
                }
            }
        } else {
            // add tab
            shift = 4
            "    $line"
        }

        if (index == 0) firstLineShift = shift
        totalShift += shift

        newLine
    }.joinToString("\n")

    // skip if nothing changes
    if (totalShift == 0) return currentValue

    // reconstruct the whole text
    val newText = textBefore + modifiedLines + textAfter

    val newMin = maxOf(startLineIndex, minCursor + firstLineShift).coerceIn(0, newText.length)
    val newMax = maxOf(newMin, maxCursor + totalShift).coerceIn(0, newText.length)


    val isReversed = rawStart > rawEnd
    val newSelection = if (isReversed) TextRange(newMax, newMin) else TextRange(newMin, newMax)

    return TextFieldValue(text = newText, selection = newSelection)
}