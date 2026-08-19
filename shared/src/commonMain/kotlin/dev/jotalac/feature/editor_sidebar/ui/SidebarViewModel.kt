package dev.jotalac.feature.editor_sidebar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.buildFileTree
import dev.jotalac.core.utils.toSafeFileName
import dev.jotalac.feature.editor.domain.EditorRepository
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.SyncStatus
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.io.files.Path
import kotlin.time.Duration.Companion.milliseconds

data class SidebarState(
    val activeNotebook: Notebook? = null,
    val fileTree: FileNode.Directory? = null,
    val expandedFolders: Set<String> = emptySet(),
    val itemToRename: String? = null
)

class EditorSidebarViewModel(
    private val notebookRepository: NotebookRepository,
    private val editorRepository: EditorRepository,
    private val snackbarManager: SnackbarManager,
    private val gitSyncRepository: GitSyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SidebarState())
    val uiState: StateFlow<SidebarState> = _uiState.asStateFlow()

    private var filesRefreshJob: Job? = null


    init {
        viewModelScope.launch {
            notebookRepository.activeNotebookState
                .distinctUntilChanged()
                .collectLatest { notebook ->
                    if (notebook != null) {

                        _uiState.update { it.copy(activeNotebook = notebook, expandedFolders = emptySet()) }

                        refreshFileTree()
                    } else {
                        _uiState.update { currentState ->
                            currentState.copy(
                                activeNotebook = null,
                                fileTree = null,
                                expandedFolders = emptySet()
                            )
                        }
                    }
                }
        }

        viewModelScope.launch {
            gitSyncRepository.syncStatus.collect { status ->
                if (status is SyncStatus.UpToDate) {
                    refreshFileTree()
                }
            }
        }
    }

    fun onAction(action: SidebarAction) {
        when (action) {
            is SidebarAction.MoveItem -> moveItem(action.sourcePath, action.destinationDirectoryPath)
            is SidebarAction.AddNote -> addNote(action.path)
            is SidebarAction.AddFolder -> addFolder(action.folderPath)
            is SidebarAction.DeleteItem -> deleteItem(action.path)
            is SidebarAction.RenameItem -> renameItem(action.path, action.newName)
            is SidebarAction.OpenNote -> setActiveNote(action.notePath)
            is SidebarAction.SetRenameItem -> setRenameItem(action.path)
            is SidebarAction.DuplicateNote -> duplicateNote(action.notePath)

            is SidebarAction.CopyItemPath -> {
                snackbarManager.showMessage("Path copied to clipboard")
            }
        }
    }

    private fun setRenameItem(path: String?) {
        _uiState.update { it.copy(itemToRename = path) }
    }

    private fun deleteItem(path: String) {
        viewModelScope.launch {
            editorRepository.deleteItem(path)
                .onSuccess {
                    notebookRepository.syncActiveNotePathOnDeleted(path)
                    refreshFileTree()
                }.onFailure {
                    snackbarManager.showMessage(it.message ?: "Failed to delete item")
                }
        }
    }

    private fun renameItem(path: String, newName: String) {
        if (newName.isBlank()) {
            setRenameItem(null)
            return
        }
        val fileExtension = Path(path).name.substringAfterLast('.', "")
        val node = findNode(path)
        val finalName = if (node is FileNode.File && fileExtension.isNotEmpty() && !newName.endsWith(".$fileExtension")) {
            "$newName.$fileExtension"
        } else {
            newName
        }
        val safeName = finalName.toSafeFileName()

        // check if the original and final names are the same
        if (node?.path?.substringAfterLast("/") == safeName) {
            setRenameItem(null)
            return
        }

        viewModelScope.launch {
            editorRepository.renameItem(path, safeName)
                .onSuccess {
                    setRenameItem(null)
                    val parentPath = Path(path).parent
                    val newPath = if (parentPath != null) Path(parentPath, safeName).toString() else safeName
                    notebookRepository.syncActiveNotePathOnMoved(path, newPath)
                    refreshFileTree()
                }.onFailure {
                    snackbarManager.showMessage(it.message ?: "Failed to rename item")
                }
        }
    }

    private fun findNode(path: String, root: FileNode? = _uiState.value.fileTree): FileNode? {
        if (root == null) return null
        if (root.path == path) return root
        if (root is FileNode.Directory) {
            for (child in root.children) {
                val found = findNode(path, child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun getUniqueName(baseName: String, isFolder: Boolean, parentPath: String?): String {
        val rootNode = _uiState.value.fileTree ?: return baseName
        val targetPath = parentPath ?: rootNode.path
        val targetDir = findNode(targetPath) as? FileNode.Directory ?: rootNode

        val extension = if (!isFolder) ".md" else ""

        fun exists(name: String): Boolean {
            return targetDir.children.any { it.name == name }
        }

        var counter = 0
        var candidate = "$baseName$extension"
        while (exists(candidate)) {
            counter++
            candidate = "$baseName $counter$extension"
        }
        return candidate
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        // debounce the file tree reload on fast alt-tabs
        if (hasFocus) {
            filesRefreshJob?.cancel()

            filesRefreshJob = viewModelScope.launch {
                delay(300.milliseconds)
                refreshFileTree()
            }
        }
    }

    private fun duplicateNote(notePath: String) {
        val fileName = notePath.substringAfterLast("/")

        addNote(notePath.substringBeforeLast("/"), fileName.substringBeforeLast("."))
    }

    private fun refreshFileTree() {
        val activeNotebook = _uiState.value.activeNotebook ?: return

        viewModelScope.launch {
            val notebookPath = Path(activeNotebook.directoryPath)
            val filesTree = withContext(Dispatchers.IO) {
                notebookPath.buildFileTree()
            }

            _uiState.update { currentState ->
                currentState.copy(fileTree = filesTree)
            }
        }
    }

    fun setActiveNote(notePath: String) {
        viewModelScope.launch {
            val result = notebookRepository.activateNote(notePath)

            result.onFailure {
                println("Failed to open note: $it")
            }
        }
    }

    fun toggleFolder(path: String) {
        _uiState.update { currentState ->
            val currentSet = currentState.expandedFolders
            val newSet = if (currentSet.contains(path)) currentSet - path else currentSet + path

            currentState.copy(
                expandedFolders = newSet,
            )
        }
    }

    fun getVisibleNodes(root: FileNode.Directory, expandedPaths: Set<String>): List<FlatFileNode> {
        val result = mutableListOf<FlatFileNode>()

        fun traverse(node: FileNode, depth: Int) {
            when (node) {
                is FileNode.File -> {
                    result.add(FlatFileNode(node, depth))
                }

                is FileNode.Directory -> {
                    val isExpanded = expandedPaths.contains(node.path)
                    result.add(FlatFileNode(node, depth, isExpanded))

                    if (isExpanded) {
                        node.children.forEach { child ->
                            traverse(child, depth + 1)
                        }
                    }
                }
            }
        }

        root.children.forEach { traverse(it, 0) }
        return result
    }

    fun expandFolder(path: String) {
        _uiState.update { currentState ->
            if (currentState.expandedFolders.contains(path)) currentState
            else currentState.copy(expandedFolders = currentState.expandedFolders + path)
        }
    }

    private fun addNote(parentPath: String?, defaultName: String = "untitled") {
        val rootPath = _uiState.value.fileTree?.path ?: return
        val targetPath = parentPath ?: rootPath
        val filename = getUniqueName(defaultName, false, targetPath)

        viewModelScope.launch {
            val result = editorRepository.addNote(filename.toSafeFileName(), targetPath)

            result.onSuccess {
                val newPath = Path(Path(targetPath), filename.toSafeFileName()).toString()
                expandFolder(targetPath)
                refreshFileTree()
                setRenameItem(newPath)
            }.onFailure {
                snackbarManager.showMessage(it.message ?: "Failed to add note")
            }
        }
    }

    private fun addFolder(parentPath: String?) {
        val rootPath = _uiState.value.fileTree?.path ?: return
        val targetPath = parentPath ?: rootPath
        val folderName = getUniqueName("untitled", true, targetPath)

        viewModelScope.launch {
            val result = editorRepository.addFolder(folderName.toSafeFileName(), targetPath)

            result.onSuccess {
                val newPath = Path(Path(targetPath), folderName.toSafeFileName()).toString()
                expandFolder(targetPath)
                refreshFileTree()
                setRenameItem(newPath)
            }.onFailure {
                snackbarManager.showMessage(it.message ?: "Failed to add folder")
            }
        }
    }

    private fun moveItem(sourcePath: String, destinationDirectoryPath: String) {
        viewModelScope.launch {
            editorRepository.moveItem(sourcePath, destinationDirectoryPath)
                .onSuccess {
                    val itemName = Path(sourcePath).name
                    val newPath = Path(Path(destinationDirectoryPath), itemName).toString()
                    notebookRepository.syncActiveNotePathOnMoved(sourcePath, newPath)
                    refreshFileTree()
                }.onFailure {
                    snackbarManager.showMessage(it.message ?: "Failed to move item")
                }
        }
    }

    fun toggleFolderCollapse() {
        if (_uiState.value.expandedFolders.isNotEmpty()) {
            // collapse
            _uiState.update {
                it.copy(
                    expandedFolders = emptySet(),
                )
            }
        } else {
            // expand
            _uiState.update { currentState ->
                val allDirs = mutableSetOf<String>()
                fun traverse(node: FileNode.Directory) {
                    allDirs.add(node.path)
                    node.children.forEach { child ->
                        if (child is FileNode.Directory) {
                            traverse(child)
                        }
                    }
                }
                currentState.fileTree?.let { traverse(it) }

                currentState.copy(expandedFolders = allDirs)
            }
        }
    }


}