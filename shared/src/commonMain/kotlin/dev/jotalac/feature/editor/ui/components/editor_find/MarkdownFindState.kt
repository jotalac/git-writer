package dev.jotalac.feature.editor.ui.components.editor_find

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class FindMatch(
    val blockIndex: Int,
    val start: Int,
    val end: Int,
)


fun findAll(blocks: List<String>, query: String, ignoreCase: Boolean = true): List<FindMatch> {
    if (query.isEmpty()) return emptyList()

    val result = mutableListOf<FindMatch>()
    blocks.forEachIndexed { index, block ->
        var from = 0
        while (true) {
            val found = block.indexOf(query, startIndex = from, ignoreCase = ignoreCase)
            if (found == -1) break
            result.add(FindMatch(index, found, found + query.length))
            from = found + query.length
        }
    }
    return result
}

/** State for the editor's find bar (Ctrl+F). */
@Stable
class MarkdownFindState {
    var isEnabled by mutableStateOf(true)
    var isOpen by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var matches by mutableStateOf<List<FindMatch>>(emptyList())
        private set
    var currentIndex by mutableStateOf(0)
        private set

    fun open() {
        isOpen = true
    }

    fun close() {
        isOpen = false
        query = ""
        matches = emptyList()
        currentIndex = 0
    }

    fun updateQuery(newQuery: String, blocks: List<String>) {
        query = newQuery
        matches = findAll(blocks, newQuery)
        currentIndex = 0
    }

    fun findNext(): FindMatch? {
        if (matches.isEmpty()) return null
        currentIndex = (currentIndex + 1) % matches.size
        return matches[currentIndex]
    }

    fun findPrevious(): FindMatch? {
        if (matches.isEmpty()) return null
        currentIndex = (currentIndex - 1 + matches.size) % matches.size
        return matches[currentIndex]
    }
}
