package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks


class EditorBlocksState {

    val blocks: SnapshotStateList<String> = mutableStateListOf()

    fun addBlock(index: Int?) {
        if (index == null) blocks.add("") else blocks.add(index, "")
    }

    fun updateBlock(index: Int, newText: String) {
        if (index !in blocks.indices) return
        blocks[index] = newText
    }

    fun removeBlock(index: Int) {
        if (index !in blocks.indices) return
        blocks.removeAt(index)
    }

    fun addBlocks(index: Int, newBlocks: List<String>) {
        blocks.addAll(index, newBlocks)
    }

    fun replaceBlockWithBlocks(index: Int, newBlocks: List<String>) {
        if (index !in blocks.indices) return
        blocks.removeAt(index)
        blocks.addAll(index, newBlocks)
    }

    fun splitBlock(index: Int, cursorStart: Int): Int? {
        if (index !in blocks.indices) return null

        val text = blocks[index]
        val chunksBefore = createChunksFromText(text.substring(0, cursorStart))
        val chunksAfter = createChunksFromText(text.substring(cursorStart))

        replaceBlockWithBlocks(index, chunksBefore + chunksAfter)
        return index + chunksBefore.size
    }

    fun mergeWithPrevious(index: Int) {
        if (index !in blocks.indices || index <= 0) return
        blocks[index - 1] = blocks[index - 1] + blocks[index]
        blocks.removeAt(index)
    }


    fun evaluateBlockOnFocusLost(index: Int, currentFocusedIndex: Int?): Int? {
        if (index >= blocks.size) return null

        val newChunks = createChunksFromText(blocks[index])
        return when {
            newChunks.isEmpty() -> {
                blocks.removeAt(index)
                if (currentFocusedIndex != null && currentFocusedIndex > index) currentFocusedIndex - 1 else null
            }

            newChunks.size > 1 -> {
                replaceBlockWithBlocks(index, newChunks)
                if (currentFocusedIndex != null && currentFocusedIndex > index) {
                    currentFocusedIndex + (newChunks.size - 1)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    fun swapBlocks(fromIndex: Int, toIndex: Int) {
        val temp = blocks[toIndex]
        blocks[toIndex] = blocks[fromIndex]
        blocks[fromIndex] = temp
    }

    fun setBlocks(newBlocks: List<String>) {
        blocks.clear()
        blocks.addAll(newBlocks)
    }

    fun insertImageBlocks(imageMarkdown: List<String>, focusedIndex: Int): Int {
        var insertIndex = if (focusedIndex in blocks.indices) focusedIndex else blocks.size

        if (insertIndex in blocks.indices && blocks[insertIndex].isBlank()) {
            blocks[insertIndex] = imageMarkdown.first()
            for (syntax in imageMarkdown.drop(1)) {
                insertIndex++
                blocks.add(insertIndex, syntax)
            }
        } else {
            for (syntax in imageMarkdown) {
                insertIndex = if (insertIndex in blocks.indices) insertIndex + 1 else blocks.size
                blocks.add(insertIndex, syntax)
            }
        }

        val targetFocusIndex = insertIndex + 1
        if (targetFocusIndex !in blocks.indices || blocks[targetFocusIndex].isNotBlank()) {
            blocks.add(targetFocusIndex, "")
        }
        return targetFocusIndex
    }

    private fun createChunksFromText(text: String): List<String> {
        val chunks = chunkMarkdownIntoBlocks(text)
        return when {
            chunks.isEmpty() && text.isNotEmpty() -> listOf(text)
            text.isEmpty() -> listOf("")
            else -> chunks
        }
    }
}
