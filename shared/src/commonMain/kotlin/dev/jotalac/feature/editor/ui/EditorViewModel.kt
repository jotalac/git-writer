package dev.jotalac.feature.editor.ui

import dev.jotalac.core.utils.RemoteStatus
import dev.jotalac.core.utils.RemoteStatusMonitor
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.data.UserSettings
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.SnackbarText
import dev.jotalac.core.utils.detectImageExtension
import dev.jotalac.core.utils.isImageFile
import dev.jotalac.feature.editor.domain.EditorRepository
import dev.jotalac.feature.editor.domain.EditorTabItem
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.encryption.EncryptedGitSync
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.err_create_note_no_notebook
import git_writer.shared.generated.resources.err_failed_create_note
import git_writer.shared.generated.resources.err_failed_save_images
import git_writer.shared.generated.resources.err_paste_image_no_notebook

class EditorViewModel(
    private val notebookRepository: NotebookRepository,
    private val editorRepository: EditorRepository,
    private val snackbarManager: SnackbarManager,
    private val userSettingsManager: UserSettings,
    gitSyncRepository: GitSyncRepository,
    private val remoteStatusMonitor: RemoteStatusMonitor,
    encryptedGitSync: EncryptedGitSync,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorScreenState())
    val uiState: StateFlow<EditorScreenState> = _uiState.asStateFlow()


    // shares the remote status, only when the app is in the foreground -> EditorScreen subscribes this flow
    val remoteStatus: StateFlow<RemoteStatus?> = remoteStatusMonitor
        .status(notebookRepository.activeNotebookState.map { it?.remoteUrl })
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // the text of the open note - Compose state, so the editor gets every change at once
    private val _noteText = mutableStateOf("")
    val noteText: State<String> = _noteText

    // where each note was scrolled to, by path, so that tab switching doesnt reset the scroll
    val noteScrollPositions = mutableMapOf<String, Int>()

    private val syncController = EditorSyncController(
        gitSyncRepository = gitSyncRepository,
        encryptedGitSync = encryptedGitSync,
        notebookRepository = notebookRepository,
        snackbarManager = snackbarManager,
        userSettingsManager = userSettingsManager,
        state = _uiState,
        saveCurrentNote = { saveNotesContent() },
        reloadNote = { reloadIfChanged(it) },
    )

    private var nextTabId = 1L

    // the note whose content is currently held in noteText
    private var loadedNotePath: String? = null

    private var autosaveJob: Job? = null

    // true from an edit until the typing has paused for TYPING_PAUSE - an automatic sync waits for that
    private val isTyping = MutableStateFlow(false)
    private var typingPauseJob: Job? = null

    init {
        // load / unload the file whenever the active note changes
        viewModelScope.launch {
            notebookRepository.activeNotePath.collect { notePath ->
                // save old files
                saveNotesContent()

                // show the loading state before the active tab's note changes
                if (notePath != null) {
                    _uiState.update { it.copy(isLoading = true) }
                }

                // keep the active tab in sync with the opened note
                applyNoteToActiveTab(notePath)

                if (notePath != null) {
                    loadFileContent(notePath)
                } else {
                    loadedNotePath = null
                    _noteText.value = ""
                    _uiState.update { it.copy(isImage = false) }
                }
            }
        }

        /**
         * reset the tabs when the active notebook changes - tabs from another notebook are stale
         * sync notes for the newly create notebook
        **/
        viewModelScope.launch {
            var previousNotebookId: Long? = null
            notebookRepository.activeNotebookState
                .map { it?.id }
                .collect { notebookId ->
                    if (previousNotebookId != null && notebookId != previousNotebookId) {
                        saveNotesContent()
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

        // auto sync: when a notebook is opened (so also on app start) and whenever the device comes back online,
        // then every 10 minutes while it stays open and online. Another notebook restarts it, turning the
        // setting off or going offline stops it
        viewModelScope.launch {
            autoSyncTarget().collectLatest { notebookId ->
                if (notebookId == null) return@collectLatest
                while (true) {
                    awaitTypingPause()
                    syncController.sync()
                    delay(AUTO_SYNC_INTERVAL)
                }
            }
        }
    }


    // the active notebook's id while auto sync is on and the notebook has a remote to sync with
    private fun autoSyncNotebookId(): Flow<Long?> = combine(
        userSettingsManager.userSettingsStateFlow.map { it.autoSync },
        notebookRepository.activeNotebookState,
    ) { autoSync, notebook ->
        notebook?.takeIf { autoSync && !it.remoteUrl.isNullOrBlank() && !it.remotePassword.isNullOrBlank() }?.id
    }.distinctUntilChanged()

    // the notebook to auto sync right now - also null while the device is offline
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun autoSyncTarget(): Flow<Long?> = autoSyncNotebookId()
        .flatMapLatest { notebookId ->
            // the connection is only watched while there is something to sync - otherwise the remote would be
            // checked in the background even with auto sync off
            if (notebookId == null) flowOf(null)
            else remoteStatus.filterNotNull().map { status -> notebookId.takeIf { status != RemoteStatus.OFFLINE } }
        }
        .distinctUntilChanged()

    // pulled changes could replace the note in the middle of a sentence - an automatic sync waits until
    // the typing has paused (without any typing it starts right away)
    private suspend fun awaitTypingPause() {
        isTyping.first { !it }
    }

    private fun onTyped() {
        isTyping.value = true
        typingPauseJob?.cancel()
        typingPauseJob = viewModelScope.launch {
            delay(TYPING_PAUSE)
            isTyping.value = false
        }
    }

    /**
     * The last sync before leaving: the app going to the background (on mobile possibly closed for good -
     * best effort, the OS may stop it) or the desktop app exiting. Only with auto sync on, and not offline -
     * it could only fail there, while the closed desktop window waits for it.
     */
    suspend fun syncBeforeLeaving() {
        if (remoteStatus.value == RemoteStatus.OFFLINE) return
        if (autoSyncNotebookId().first() != null) syncController.sync()
    }

    // after a sync - reloading resets the editor (focus, cursor), so only when the pull changed the open note
    private suspend fun reloadIfChanged(notePath: String) {
        val onDisk = editorRepository.readNoteContent(notePath).getOrNull()
        if (onDisk == null || onDisk.normalizeLineEndings() != _noteText.value) {
            loadFileContent(notePath)
        }
    }


     // Autosave is triggered by the edits of the note
    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(1.seconds)
            saveNotesContent()
        }
    }

    suspend fun loadFileContent(filePath: String) {
        _uiState.update { it.copy(isLoading = true) }

        // check if the file is valid before loading
        if (!editorRepository.fileExists(filePath)) {
            _uiState.update {
                it.copy(error = "Error loading file - $filePath", isLoading = false)
            }
            return
        }

        val isImage = isImageFile(filePath.substringAfterLast("/"))

        val content = if (isImage) "" else editorRepository.readNoteContent(filePath).getOrElse {
            // nothing is loaded, so nothing may be saved over the note either
            loadedNotePath = null
            _noteText.value = ""
            _uiState.update { state -> state.copy(error = "Error loading file - $filePath", isLoading = false) }
            return
        }

        loadedNotePath = filePath
        _noteText.value = content.normalizeLineEndings()

        _uiState.update { it.copy(isLoading = false, isImage = isImage) }
    }

    private suspend fun saveNotesContent() {
        val notePath = loadedNotePath ?: return

        if (!_uiState.value.isLoading && !_uiState.value.isImage) {
            editorRepository.saveFile(_noteText.value, notePath)
        }
    }

    // --- tabs ---

    fun openTab(id: Long) {
        val state = _uiState.value
        val tab = state.openedTabs.firstOrNull { it.id == id } ?: return
        if (tab.id == state.activeTabId) return

        viewModelScope.launch {
            saveNotesContent()

            val loadsDifferentNote = tab.notePath != null && tab.notePath != state.activeNotePath

            _uiState.update { it.copy(activeTabId = tab.id, isLoading = loadsDifferentNote) }

            when {
                tab.notePath == null -> notebookRepository.closeActiveNote()
                loadsDifferentNote -> notebookRepository.activateNote(tab.notePath)
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
            saveNotesContent()

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
            saveNotesContent()

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
            val newActiveNote = newActiveTab.notePath
            val loadsDifferentNote = newActiveNote != null && newActiveNote != state.activeNotePath

            _uiState.update {
                it.copy(openedTabs = newTabs, activeTabId = newActiveTab.id, isLoading = loadsDifferentNote)
            }

            when {
                newActiveNote == null -> notebookRepository.closeActiveNote()
                loadsDifferentNote -> notebookRepository.activateNote(newActiveNote)
                else -> Unit // same note as the current one - no reload needed
            }
        }
    }

    fun closeActiveTab() {
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
                snackbarManager.showMessage(SnackbarText.resource(Res.string.err_create_note_no_notebook))
                return@launch
            }

            editorRepository.createNote(notebookRootPath)
                .onSuccess { newPath -> notebookRepository.activateNote(newPath) }
                .onFailure { snackbarManager.showMessage(SnackbarText.message(Res.string.err_failed_create_note, it.message)) }
        }
    }

    // --- actions ---

    fun onAction(action: EditorAction) {
        when (action) {
            is EditorAction.UpdateText -> _noteText.value = action.text
            is EditorAction.PasteImages -> savePastedImages(action.imageBytesList, action.onImagesSaved)

            is EditorAction.SyncNotes -> viewModelScope.launch { syncController.sync() }
            is EditorAction.SyncBeforeLeaving -> viewModelScope.launch { syncBeforeLeaving() }
            is EditorAction.CheckRemoteStatus -> remoteStatusMonitor.checkNow()
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

        if (action.isNoteEditing()) {
            onTyped()
            scheduleAutosave()
        }
    }

    private fun savePastedImages(
        imageBytesList: List<ByteArray>,
        onImagesSaved: (imageMarkdown: List<String>) -> Unit,
    ) {
        if (imageBytesList.isEmpty()) return

        viewModelScope.launch {
            val notebookRootPath = notebookRepository.activeNotebookState.firstOrNull()?.directoryPath

            if (notebookRootPath == null) {
                snackbarManager.showMessage(SnackbarText.resource(Res.string.err_paste_image_no_notebook))
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
                    snackbarManager.showMessage(SnackbarText.resource(Res.string.err_failed_save_images))
                } else {
                    savedMarkdownSyntaxes.add("![pasted image](images/$filename)")
                }
            }

            if (savedMarkdownSyntaxes.isEmpty()) return@launch

            onImagesSaved(savedMarkdownSyntaxes)
        }
    }
}

private val AUTO_SYNC_INTERVAL = 10.minutes
private val TYPING_PAUSE = 3.seconds

// the editor works with "\n" only - notes written on Windows have "\r\n"
private fun String.normalizeLineEndings(): String = replace("\r\n", "\n").replace("\r", "\n")
