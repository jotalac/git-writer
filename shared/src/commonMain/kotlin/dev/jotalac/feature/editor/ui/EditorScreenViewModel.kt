package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class EditorScreenState(
    val activeFilename: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class EditorViewModel(
    private val notebookRepository: NotebookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorScreenState())
    val uiState: StateFlow<EditorScreenState> = _uiState.asStateFlow()

    val markdownBlocks = mutableStateListOf<String>()

    // load the file data
    init {
        viewModelScope.launch {
            notebookRepository.activeNotePath.collect { notePath ->
                if (notePath != null) {
                    //load file content
                    loadFileContent(notePath)
                } else {
                    // 'unload' the file
                    markdownBlocks.clear()
                    _uiState.update {
                        it.copy(activeFilename = null)
                    }
                }
            }
        }

    }

    suspend fun loadFileContent(filePath: String) {
        _uiState.update { it.copy(isLoading = true) }

        val file = PlatformFile(filePath)

        // check if the file is valid before loading it
        if (!file.exists() || !file.isRegularFile()) {
            _uiState.update {
                it.copy(error = "Error loading file - $filePath", isLoading = false)
            }
            return
        }

        val fileContent = file.readString()

        val initialChunks = chunkMarkdownIntoBlocks(fileContent)
        markdownBlocks.clear()
        markdownBlocks.addAll(initialChunks)

        _uiState.update { it.copy(isLoading = false, activeFilename = file.name) }

    }

    fun closeActiveNote() {
        viewModelScope.launch {
            val result = notebookRepository.closeActiveNote()

            result.onFailure {
                println("Failed to close active note - $it")
            }
        }
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

//                if (text.isBlank()) {
//                    // Block is empty - remove it
//                    markdownBlocks.removeAt(action.index)
//                    if (currentFocused != null && currentFocused > action.index) {
//                        action.onFocusAdjusted(currentFocused - 1)
//                    }
//                } else {
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
//                }
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