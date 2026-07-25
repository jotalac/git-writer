package dev.jotalac.feature.editor_sidebar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jotalac.core.utils.buildFileTree
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlin.time.Duration.Companion.milliseconds

data class SidebarState(
    val activeNotebook: Notebook? = null,
    val fileTree: FileNode.Directory? = null,
    val expandedFolders: Set<String> = emptySet(),
)

class EditorSidebarViewModel(
    private val notebookRepository: NotebookRepository,
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


}