package dev.jotalac.feature.editor.ui

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.data.UserSettingsManager
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.detectImageExtension
import dev.jotalac.core.utils.isImageFile
import dev.jotalac.feature.editor.domain.EditorRepository
import dev.jotalac.feature.editor.domain.EditorTabItem
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
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

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val notebookRepository: NotebookRepository,
    private val editorRepository: EditorRepository,
    private val snackbarManager: SnackbarManager,
    private val userSettingsManager: UserSettingsManager,
    gitSyncRepository: GitSyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorScreenState())
    val uiState: StateFlow<EditorScreenState> = _uiState.asStateFlow()

    private val blocksState = EditorBlocksState()
    val markdownBlocks = blocksState.blocks

    private val syncController = EditorSyncController(
        gitSyncRepository = gitSyncRepository,
        notebookRepository = notebookRepository,
        snackbarManager = snackbarManager,
        userSettingsManager = userSettingsManager,
        state = _uiState,
        saveCurrentNote = { saveNotesContent(markdownBlocks) },
        reloadNote = { loadFileContent(it) },
    )

    private var nextTabId = 1L

    // the note whose content is currently held in markdownBlocks
    private var loadedNotePath: String? = null

    init {
        // load / unload the file whenever the active note changes
        viewModelScope.launch {
            notebookRepository.activeNotePath.collect { notePath ->
                // save old files
                saveNotesContent(markdownBlocks)

                // keep the active tab in sync with the opened note
                applyNoteToActiveTab(notePath)

                if (notePath != null) {
                    loadFileContent(notePath)
                } else {
                    loadedNotePath = null
                    markdownBlocks.clear()
                    _uiState.update { it.copy(isImage = false) }
                }
            }
        }

        // reset the tabs when the active notebook changes - tabs from another notebook are stale
        viewModelScope.launch {
            var previousNotebookId: Long? = null
            notebookRepository.activeNotebookState
                .map { it?.id }
                .collect { notebookId ->
                    if (previousNotebookId != null && notebookId != previousNotebookId) {
                        saveNotesContent(markdownBlocks)
                        resetTabs()
                    }
                    previousNotebookId = notebookId
                }
        }

        // mirror the sync status into the UI
        viewModelScope.launch {
            gitSyncRepository.gitSyncStatus.collect { status ->
                _uiState.update { it.copy(gitSyncStatus = status) }
            }
        }

        // debounced autosave
        viewModelScope.launch {
            snapshotFlow { markdownBlocks.toList() }
                .debounce(1.seconds)
                .distinctUntilChanged()
                .collectLatest { currentBlocks ->
                    saveNotesContent(currentBlocks)
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

        val isImage = isImageFile(file.name)

        loadedNotePath = filePath

        if (isImage) {
            markdownBlocks.clear()
        } else {
            val loadResult = editorRepository.loadMarkdownFileBlocks(file)
            markdownBlocks.clear()
            loadResult.onSuccess { blocks -> markdownBlocks.addAll(blocks) }
        }

        _uiState.update { it.copy(isLoading = false, isImage = isImage) }
    }

    private suspend fun saveNotesContent(currentBlocks: List<String>) {
        val notePath = loadedNotePath ?: return

        if (!_uiState.value.isLoading && !_uiState.value.isImage) {
            val contentToSave = currentBlocks.joinToString("\n\n")
            editorRepository.saveFile(contentToSave, notePath)
        }
    }

    // --- tabs ---

    fun openTab(id: Long) {
        val state = _uiState.value
        val tab = state.openedTabs.firstOrNull { it.id == id } ?: return
        if (tab.id == state.activeTabId) return

        viewModelScope.launch {
            saveNotesContent(markdownBlocks)

            // switch the active tab first, so the note change emitted below is applied to the right tab
            _uiState.update { it.copy(activeTabId = tab.id) }

            when {
                tab.notePath == null -> notebookRepository.closeActiveNote()
                tab.notePath != state.activeNotePath -> notebookRepository.activateNote(tab.notePath)
                else -> Unit // same note as the current one - only the tab changes
            }
        }
    }

    fun openNextTab() {
        val state = _uiState.value
        val currentIndex = state.openedTabs.indexOfFirst { it.id == state.activeTabId }
        if (currentIndex == -1) return
        val next = state.openedTabs.getOrNull(currentIndex + 1) ?: state.openedTabs.first()
        openTab(next.id)
    }

    fun openPreviousTab() {
        val state = _uiState.value
        val currentIndex = state.openedTabs.indexOfFirst { it.id == state.activeTabId }
        if (currentIndex == -1) return
        val previous = state.openedTabs.getOrNull(currentIndex - 1) ?: state.openedTabs.last()
        openTab(previous.id)
    }

    fun addNewTab() {
        viewModelScope.launch {
            saveNotesContent(markdownBlocks)

            val newTab = EditorTabItem(id = nextTabId++, notePath = null)
            _uiState.update {
                it.copy(openedTabs = it.openedTabs + newTab, activeTabId = newTab.id)
            }
            notebookRepository.closeActiveNote()
        }

    }

    fun closeTab(id: Long) {
        val state = _uiState.value
        if (state.openedTabs.none { it.id == id }) return

        viewModelScope.launch {
            saveNotesContent(markdownBlocks)

            if (state.openedTabs.size == 1) {
                // closing the only tab: replace it with a fresh empty one so at least one tab always exists
                val newTab = EditorTabItem(id = nextTabId++, notePath = null)
                _uiState.update { it.copy(openedTabs = listOf(newTab), activeTabId = newTab.id) }
                notebookRepository.closeActiveNote()
                return@launch
            }

            val closedIndex = state.openedTabs.indexOfFirst { it.id == id }
            val wasActive = id == state.activeTabId
            val newTabs = state.openedTabs.filterNot { it.id == id }

            if (!wasActive) {
                _uiState.update { it.copy(openedTabs = newTabs) }
                return@launch
            }

            // the active tab was closed: activate the tab that took its place (or the previous one)
            val newActiveTab = newTabs.getOrNull(closedIndex) ?: newTabs.last()
            _uiState.update { it.copy(openedTabs = newTabs, activeTabId = newActiveTab.id) }

            val newActiveNote = newActiveTab.notePath
            when {
                newActiveNote == null -> notebookRepository.closeActiveNote()
                newActiveNote != state.activeNotePath -> notebookRepository.activateNote(newActiveNote)
                else -> Unit // same note as the current one - no reload needed
            }
        }
    }

    fun closeActiveTab() {
        println("Active tab id: ${_uiState.value.activeTabId}")
        closeTab(_uiState.value.activeTabId)
    }

    private fun applyNoteToActiveTab(notePath: String?) {
        // when the note is opened already somewhere else, just open that tab
        if (notePath != null) {
            val existingTab = _uiState.value.openedTabs.firstOrNull { it.notePath == notePath }
            if (existingTab != null) {
                _uiState.update { it.copy(activeTabId = existingTab.id) }
                return
            }
        }

        _uiState.update { state ->
            state.copy(
                openedTabs = state.openedTabs.map { tab ->
                    if (tab.id == state.activeTabId) tab.copy(notePath = notePath) else tab
                }
            )
        }
    }

    private fun resetTabs() {
        val newTab = EditorTabItem(id = nextTabId++, notePath = null)
        _uiState.update { it.copy(openedTabs = listOf(newTab), activeTabId = newTab.id) }
    }

    private fun createNewNote() {
        viewModelScope.launch {
            val notebookRootPath = notebookRepository.activeNotebookState.firstOrNull()?.directoryPath
            if (notebookRootPath == null) {
                snackbarManager.showMessage("Failed to create note: no active notebook")
                return@launch
            }

            editorRepository.createNote(notebookRootPath)
                .onSuccess { newPath -> notebookRepository.activateNote(newPath) }
                .onFailure { snackbarManager.showMessage(it.message ?: "Failed to create note") }
        }
    }

    // --- actions ---

    fun onAction(action: EditorAction) {
        when (action) {
            is EditorAction.AddBlock -> blocksState.addBlock(action.index)
            is EditorAction.UpdateBlock -> blocksState.updateBlock(action.index, action.newText)
            is EditorAction.RemoveBlock -> blocksState.removeBlock(action.index)
            is EditorAction.AddBlocks -> blocksState.addBlocks(action.index, action.newBlocks)
            is EditorAction.BlockTurnedIntoMoreBlocks -> blocksState.replaceBlockWithBlocks(
                action.index,
                action.newBlocks
            )

            is EditorAction.SplitBlock -> blocksState.splitBlock(action.index, action.cursorStart)
                ?.let(action.onFocusCalculated)

            is EditorAction.MergeWithPrevBlock -> blocksState.mergeWithPrevious(action.index)
            is EditorAction.EvaluateBlockOnFocusLost ->
                blocksState.evaluateBlockOnFocusLost(action.index, action.currentFocusedIndex)
                    ?.let(action.onFocusAdjusted)

            is EditorAction.SwapBlocks -> blocksState.swapBlocks(action.fromIndex, action.toIndex)
            is EditorAction.SetBlocks -> blocksState.setBlocks(action.blocks)
            is EditorAction.PasteImages -> savePastedImages(
                action.imageBytesList,
                action.focusedIndex,
                action.onFocusCalculated
            )

            is EditorAction.SyncNotes -> viewModelScope.launch { syncController.sync() }
            is EditorAction.ResolveSingleConflict -> viewModelScope.launch {
                syncController.resolveSingleConflict(action.filePath, action.keepLocalChanges)
            }

            is EditorAction.ResolveAllConflicts -> viewModelScope.launch {
                syncController.resolveAllConflicts(action.keepLocalChanges)
            }

            is EditorAction.AbortConflictResolve -> viewModelScope.launch { syncController.abort() }

            is EditorAction.CloseActiveTab -> closeActiveTab()
            is EditorAction.NewTab -> addNewTab()
            is EditorAction.NextTab -> openNextTab()
            is EditorAction.PreviousTab -> openPreviousTab()
            is EditorAction.NewNote -> createNewNote()
        }
    }

    private fun savePastedImages(
        imageBytesList: List<ByteArray>,
        focusedIndex: Int,
        onFocusCalculated: (Int) -> Unit
    ) {
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
                    savedMarkdownSyntaxes.add("![pasted image](images/$filename)")
                }
            }

            if (savedMarkdownSyntaxes.isEmpty()) return@launch

            val targetFocusIndex = blocksState.insertImageBlocks(savedMarkdownSyntaxes, focusedIndex)
            onFocusCalculated(targetFocusIndex)
        }
    }
}
