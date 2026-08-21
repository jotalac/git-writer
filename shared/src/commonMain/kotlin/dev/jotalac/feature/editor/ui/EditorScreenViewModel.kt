package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.detectImageExtension
import dev.jotalac.core.utils.isImageFile
import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks
import dev.jotalac.feature.editor.domain.EditorRepository
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

data class EditorScreenState(
    val activeFilename: String? = null,
    val activeNotePath: String? = null,
    val isImage: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val gitSyncStatus: GitSyncStatus = GitSyncStatus.UpToDate,
    val conflictedFiles: List<String> = emptyList(),
)

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val notebookRepository: NotebookRepository,
    private val editorRepository: EditorRepository,
    private val snackbarManager: SnackbarManager,
    private val gitSyncRepository: GitSyncRepository,
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

        viewModelScope.launch {
            gitSyncRepository.gitSyncStatus.collect { status ->
                _uiState.update { it.copy(gitSyncStatus = status) }
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

    fun syncNotes() {
        viewModelScope.launch {
            val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return@launch
            if (notebook.remoteUrl.isNullOrBlank() || notebook.remotePassword.isNullOrBlank()) return@launch

            _uiState.update { it.copy(gitSyncStatus = GitSyncStatus.Syncing) }

            saveNotesContent(markdownBlocks)

            val result = gitSyncRepository.syncNotes(
                notebook.directoryPath,
                notebook.remotePassword,
                notebook.remoteUsername
            )

            result.onSuccess { syncResult ->
                // check if we need to resolve conflicts
                if (syncResult is GitSyncStatus.Conflict) {
                    _uiState.update { state ->
                        state.copy(conflictedFiles = syncResult.files.toList())
                    }
                } else {
                    val activePath = _uiState.value.activeNotePath
                    if (activePath != null) {
                        loadFileContent(activePath)
                    }
                    _uiState.update { it.copy(gitSyncStatus = GitSyncStatus.UpToDate) }
                    snackbarManager.showMessage("Notes synced successfully")
                }
            }.onFailure { errorResult ->
                _uiState.update { it.copy(gitSyncStatus = GitSyncStatus.GitSyncFailed) }
                snackbarManager.showMessage(errorResult.message ?: "Error syncing notes")
            }
        }
    }

    fun resolveSingleConflict(filePath: String, keepLocal: Boolean) {
        viewModelScope.launch {
            val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return@launch
            val result = gitSyncRepository.resolveSingleConflict(
                currentNotebookPath = notebook.directoryPath,
                conflictedFilePath = filePath,
                keepLocalChanges = keepLocal
            )
            result.onSuccess {
                val remaining = _uiState.value.conflictedFiles.filter { it != filePath }
                _uiState.update { it.copy(conflictedFiles = remaining) }

                // if there was conflict resolved on the opened note - refresh the note
                val activeNotePath = _uiState.value.activeNotePath
                if (activeNotePath != null && (activeNotePath.endsWith(filePath) || _uiState.value.activeFilename == filePath)) {
                    loadFileContent(activeNotePath)
                }

                if (remaining.isEmpty()) {
                    if (!notebook.remotePassword.isNullOrBlank()) {
                        gitSyncRepository.pushChanges(
                            currentNotebookPath = notebook.directoryPath,
                            tokenOrPassword = notebook.remotePassword,
                            username = notebook.remoteUsername
                        )
                    }
                    snackbarManager.showMessage("All conflicts resolved and synced")
                }
            }.onFailure {
                snackbarManager.showMessage(it.message ?: "Failed to resolve conflict")
            }
        }
    }

    fun resolveAllConflicts(keepLocal: Boolean) {
        viewModelScope.launch {
            val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return@launch
            val result = gitSyncRepository.resolveAllConflicts(
                currentNotebookPath = notebook.directoryPath,
                keepLocalChanges = keepLocal
            )
            result.onSuccess {
                _uiState.update { it.copy(conflictedFiles = emptyList()) }

                // reload the opened note
                val currentActive = _uiState.value.activeNotePath
                if (currentActive != null) {
                    loadFileContent(currentActive)
                }

                if (!notebook.remotePassword.isNullOrBlank()) {
                    gitSyncRepository.pushChanges(
                        currentNotebookPath = notebook.directoryPath,
                        tokenOrPassword = notebook.remotePassword,
                        username = notebook.remoteUsername
                    )
                }
                snackbarManager.showMessage("All conflicts resolved and synced")
            }.onFailure {
                snackbarManager.showMessage(it.message ?: "Failed to resolve conflicts")
            }
        }
    }

    fun dismissConflictDialog() {
        viewModelScope.launch {
            val notebook = notebookRepository.activeNotebookState.firstOrNull() ?: return@launch
            val result = gitSyncRepository.abortMerge(notebook.directoryPath)

            result.onSuccess {
                snackbarManager.showMessage("Sync aborted")
                _uiState.update { it.copy(conflictedFiles = emptyList()) }
            }.onFailure {
                snackbarManager.showMessage(it.message ?: "Failed to abort sync")
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

            is EditorAction.MergeWithPrevBlock -> {
                if (action.index !in markdownBlocks.indices || action.index <= 0) return
                markdownBlocks[action.index - 1] = markdownBlocks[action.index - 1] + markdownBlocks[action.index]
                markdownBlocks.removeAt(action.index)
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

            is EditorAction.PasteImages -> {
                savePastedImages(action.imageBytesList, action.focusedIndex, action.onFocusCalculated)
            }

            is EditorAction.SwapBlocks -> {
                val temp = markdownBlocks[action.toIndex]
                markdownBlocks[action.toIndex] = markdownBlocks[action.fromIndex]
                markdownBlocks[action.fromIndex] = temp
            }

            is EditorAction.SyncNotes -> {
                syncNotes()
            }

            is EditorAction.ResolveSingleConflict -> {
                resolveSingleConflict(action.filePath, action.keepLocalChanges)
            }

            is EditorAction.ResolveAllConflicts -> {
                resolveAllConflicts(action.keepLocalChanges)
            }

            is EditorAction.AbortConflictResolve -> {
                dismissConflictDialog()
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

    fun savePastedImages(imageBytesList: List<ByteArray>, focusedIndex: Int, onFocusCalculated: (Int) -> Unit) {
        if (imageBytesList.isEmpty()) return

        viewModelScope.launch {
            val notebookRootPath = notebookRepository.activeNotebookState.firstOrNull()?.directoryPath

            if (notebookRootPath == null) {
                snackbarManager.showMessage("Failed to paste image: no active notebook")
                return@launch
            }

            val savedMarkdownSyntaxes = mutableListOf<String>()
            val now = Clock.System.now().toEpochMilliseconds()

            for ((index, imageBytes) in imageBytesList.withIndex()) {
                val extension = imageBytes.detectImageExtension()
                val randomSuffix = Random.nextInt(1000, 9999)
                val filename = "pasted_image_${now}_${index}_$randomSuffix$extension"

                val result = editorRepository.savePastedImage(notebookRootPath, imageBytes, filename)
                if (result.isFailure) {
                    snackbarManager.showMessage("Failed to save one or more pasted images")
                } else {
                    val relativePath = "images/$filename"
                    savedMarkdownSyntaxes.add("![pasted image]($relativePath)")
                }
            }

            if (savedMarkdownSyntaxes.isEmpty()) return@launch

            var insertIndex = if (focusedIndex in markdownBlocks.indices) focusedIndex else markdownBlocks.size
            if (insertIndex in markdownBlocks.indices && markdownBlocks[insertIndex].isBlank()) {
                markdownBlocks[insertIndex] = savedMarkdownSyntaxes.first()
                for (syntax in savedMarkdownSyntaxes.drop(1)) {
                    insertIndex++
                    markdownBlocks.add(insertIndex, syntax)
                }
            } else {
                for (syntax in savedMarkdownSyntaxes) {
                    insertIndex = if (insertIndex in markdownBlocks.indices) insertIndex + 1 else markdownBlocks.size
                    markdownBlocks.add(insertIndex, syntax)
                }
            }

            val targetFocusIndex: Int
            if (insertIndex + 1 < markdownBlocks.size && markdownBlocks[insertIndex + 1].isBlank()) {
                targetFocusIndex = insertIndex + 1
            } else {
                targetFocusIndex = insertIndex + 1
                markdownBlocks.add(targetFocusIndex, "")
            }
            onFocusCalculated(targetFocusIndex)
        }
    }
}