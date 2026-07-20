package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class EditorScreenState(
    val filename: String? = null,
    val isLoading: Boolean = true,
)

class EditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditorScreenState())
    val uiState: StateFlow<EditorScreenState> = _uiState.asStateFlow()

    val markdownBlocks = mutableStateListOf<String>()

    // load the file data
    init {
        val receivedFilename = "test.md"
        _uiState.update { currentState ->
            currentState.copy(
                filename = receivedFilename,
            )
        }

        viewModelScope.launch {
            loadFileContent(receivedFilename)

        }

    }

    suspend fun loadFileContent(filename: String) {
        val rawText = """
        # Hello Markdown

        This is a simple markdown example with:

        - Bullet points
        - **Bold text**
        - *Italic text*

        ```kotlin
        val myValue = 10
        fun thisIsFunction()
        ```

        [Check out this link](https://github.com/mikepenz/multiplatform-markdown-renderer)
        """.trimIndent()

        val initialChunks = chunkMarkdownIntoBlocks(rawText)
        markdownBlocks.clear()
        markdownBlocks.addAll(initialChunks)

        delay(1000.milliseconds)

        _uiState.update { it.copy(isLoading = false) }

    }

    fun onAction(action: EditorAction) {
        when (action) {
            is EditorAction.AddBlock -> {
                if (action.index == null) {
                    markdownBlocks.add("")
                } else {
                    markdownBlocks.add(action.index, "")
                }
            }
            is EditorAction.UpdateBlock -> {
                markdownBlocks[action.index] = action.newText
            }
            is EditorAction.RemoveBlock -> {
                markdownBlocks.removeAt(action.index)
            }
            is EditorAction.AddBlocks -> {
                markdownBlocks.addAll(action.index, action.newBlocks)
            }
            is EditorAction.BlockTurnedIntoMoreBlocks -> {
                addNewSplitBlocks(action.index, action.newBlocks)
            }
            is EditorAction.SplitBlock -> {
                val text = markdownBlocks[action.index]
                val textBefore = text.substring(0, action.cursorStart)
                val textAfter = text.substring(action.cursorStart)

                // split to text before cursor and after cursor (create valid blocks from the text before and after cursor)
                val chunksBefore = createChunksFromText(textBefore)
                val chunksAfter = createChunksFromText(textAfter)

                addNewSplitBlocks(action.index, chunksBefore + chunksAfter)

                val focusIndex = action.index + chunksAfter.size
                action.onFocusCalculated(focusIndex)
            }
            is EditorAction.EvaluateBlockOnFocusLost -> {
                if (action.index >= markdownBlocks.size) return

                val text = markdownBlocks[action.index]
                val currentFocused = action.currentFocusedIndex

                if (text.isBlank()) {
                    // Block is empty - remove it
                    markdownBlocks.removeAt(action.index)
                    if (currentFocused != null && currentFocused > action.index) {
                        action.onFocusAdjusted(currentFocused - 1)
                    }
                } else {
                    // Block has text - check if it needs to be chunked
                    val newChunks = createChunksFromText(text)

                    if (newChunks.isEmpty()) {
                        markdownBlocks.removeAt(action.index)
                        if (currentFocused != null && currentFocused > action.index) {
                            action.onFocusAdjusted(currentFocused - 1)
                        }
                    } else if (newChunks.size > 1) {
                        // Block split into multiple chunks
                        markdownBlocks.removeAt(action.index)
                        markdownBlocks.addAll(action.index, newChunks)

                        if (currentFocused != null && currentFocused > action.index) {
                            action.onFocusAdjusted(currentFocused + (newChunks.size - 1))
                        }
                    }
                }
            }
            else -> {
                println("Invalid action")
            }
        }
    }

    private fun addNewSplitBlocks(index: Int, newBlocks: List<String>) {
        markdownBlocks.removeAt(index)
        markdownBlocks.addAll(index, newBlocks)
    }


    private fun createChunksFromText(text: String): List<String> {
        val chunks = chunkMarkdownIntoBlocks(text)
        return if (chunks.isEmpty() && text.isNotEmpty()) {
            listOf(text)
        } else if (text.isEmpty()) {
            listOf("")
        } else {
            chunks
        }
    }



}