package dev.jotalac.feature.editor_sidebar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.utils.buildClipEntry
import dev.jotalac.feature.editor_sidebar.ui.components.ActiveNotebookMenu
import dev.jotalac.feature.editor_sidebar.ui.components.SidebarGlobalActions
import dev.jotalac.feature.editor_sidebar.ui.components.file_tree.FileTree
import dev.jotalac.feature.notebooks_management.ui.create_notebook.CreateNotebookDialog
import dev.jotalac.feature.notebooks_management.ui.edit_notebook.EditNotebookDialog
import dev.jotalac.feature.notebooks_management.ui.list_notebooks.NotebookListDialog
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun SidebarContent(
    viewModel: EditorSidebarViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    closeSidebarOnNoteOpen: () -> Unit = {},
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var showEditNotebookDialog by remember { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    //refresh the files tree on window focus change
    val windowInfo = LocalWindowInfo.current
    val isWindowFocused = windowInfo.isWindowFocused

    LaunchedEffect(isWindowFocused) {
        viewModel.onWindowFocusChanged(isWindowFocused)
    }


    if (showCreateDialog) {
        CreateNotebookDialog(
            onDismiss = { showCreateDialog = false },
        )
    }

    if (showListDialog) {
        NotebookListDialog(
            activeNotebookId = state.activeNotebook?.id,
            onDismiss = { showListDialog = false }
        )
    }

    if (showEditNotebookDialog && state.activeNotebook != null) {
        EditNotebookDialog(
            notebook = state.activeNotebook!!,
            onDismiss = { showEditNotebookDialog = false }
        )
    }


    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        SidebarGlobalActions(
            onNotebookOpen = { showListDialog = true },
            onNotebookCreate = { showCreateDialog = true },
            onSettingsOpen = {},
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        ActiveNotebookMenu(
            notebookName = state.activeNotebook?.name,
            onCollapseToggled = viewModel::toggleFolderCollapse,
            anyFolderExpanded = state.expandedFolders.isNotEmpty(),
            onAddNoteClick = { viewModel.onAction(SidebarAction.AddNote()) },
            onAddFolderClick = { viewModel.onAction(SidebarAction.AddFolder()) },
            openEditNotebookDialog = { showEditNotebookDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))


        //display the folders content
        if (state.fileTree != null) {
            val visibleItems = remember(state.fileTree, state.expandedFolders) {
                viewModel.getVisibleNodes(state.fileTree!!, state.expandedFolders)
            }

            FileTree(
                visibleItems = visibleItems,
                rootPath = state.fileTree!!.path,
                onFolderToggle = { viewModel.toggleFolder(it) },
                itemToRename = state.itemToRename,
                onAction = { action ->
                    // handle the copy item with clipboard manager
                    if (action is SidebarAction.CopyItemPath) {
                        scope.launch {
                            clipboardManager.setClipEntry(buildClipEntry(action.path))
                        }
                    }
                    // close the sidebar
                    if (action is SidebarAction.OpenNote) closeSidebarOnNoteOpen()
                    viewModel.onAction(action)
                }
            )
        }
    }
}