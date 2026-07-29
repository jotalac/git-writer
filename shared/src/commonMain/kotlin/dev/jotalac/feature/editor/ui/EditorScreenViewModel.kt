package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.utils.isImageFile
import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks
import dev.jotalac.feature.editor.domain.EditorRepository
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class EditorScreenState(
    val activeFilename: String? = null,
    val activeNotePath: String? = null,
    val isImage: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val notebookRepository: NotebookRepository,
    private val editorRepository: EditorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorScreenState())
    val uiState: StateFlow<EditorScreenState> = _uiState.asStateFlow()

    val markdownBlocks = mutableStateListOf<String>()


    init {
        // load the file data
        viewModelScope.launch {
            // handle the file changing rendering
            notebookRepository.activeNotePath.collect { notePath ->
                //save old files
                saveNotesContent(markdownBlocks)

                if (notePath != null) {
                    //load file content
                    loadFileContent(notePath)
                } else {
                    // 'unload' the file
                    markdownBlocks.clear()
                    _uiState.update {
                        it.copy(activeFilename = null, activeNotePath = null, isImage = false)
                    }
                }
            }


        }

        // handle file saving
        viewModelScope.launch {
            snapshotFlow { markdownBlocks.toList() }
                .debounce(1.seconds) // save every 1s debounced
                .distinctUntilChanged() // dont do anything when the content didnt changed
                .collectLatest { currentBlocks ->
                    saveNotesContent(currentBlocks)
                }
        }

    }

    suspend fun loadFileContent(filePath: String) {
        _uiState.update { it.copy(isLoading = true, activeNotePath = filePath) }

        val file = PlatformFile(filePath)

        // check if the file is valid before loading it
        if (!file.exists() || !file.isRegularFile()) {
            _uiState.update {
                it.copy(error = "Error loading file - $filePath", isLoading = false)
            }
            return
        }

        val filename = file.name
        val isImage = isImageFile(filename)

        if (isImage) {
            markdownBlocks.clear()
        } else {
            val loadResult = editorRepository.loadMarkdownFileBlocks(file)
            markdownBlocks.clear()
            loadResult.onSuccess { blocks -> markdownBlocks.addAll(blocks) }
        }

        _uiState.update {
            it.copy(isLoading = false, activeFilename = filename, isImage = isImage)
        }
    }

    private suspend fun saveNotesContent(currentBlocks: List<String>) {
        val currentState = _uiState.value

        if (currentState.activeNotePath != null &&
            !currentState.isLoading &&
            !currentState.isImage
        ) {
            val contentToSave = currentBlocks.joinToString("\n\n")
            editorRepository.saveFile(contentToSave, currentState.activeNotePath)
        }
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
                if (action.index !in markdownBlocks.indices) return
                markdownBlocks[action.index] = action.newText
            }

            is EditorAction.RemoveBlock -> {
                if (action.index !in markdownBlocks.indices) return
                markdownBlocks.removeAt(action.index)
            }

            is EditorAction.AddBlocks -> {
                markdownBlocks.addAll(action.index, action.newBlocks)
            }

            is EditorAction.BlockTurnedIntoMoreBlocks -> {
                if (action.index !in markdownBlocks.indices) return
                addNewSplitBlocks(action.index, action.newBlocks)
            }

            is EditorAction.SplitBlock -> {
                if (action.index !in markdownBlocks.indices) return
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