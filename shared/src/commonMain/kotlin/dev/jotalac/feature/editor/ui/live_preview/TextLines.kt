package dev.jotalac.feature.editor.ui.live_preview

/** Where the line with [offset] starts. */
internal fun CharSequence.lineStartAt(offset: Int): Int = lastIndexOf('\n', offset - 1) + 1

/** Where the line with [offset] ends - before its line break. */
internal fun CharSequence.lineEndAt(offset: Int): Int = indexOf('\n', offset).let { if (it == -1) length else it }
