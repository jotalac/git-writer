package dev.jotalac.feature.editor_sidebar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.ui.components.ActiveNotebookMenu
import dev.jotalac.feature.editor_sidebar.ui.components.SidebarGlobalActions
import dev.jotalac.feature.editor_sidebar.ui.components.file_tree.SidebarFileTree
import dev.jotalac.feature.notebooks_management.ui.CreateNotebookDialog
import dev.jotalac.feature.notebooks_management.ui.NotebookListDialog
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun SidebarContent(
    viewModel: EditorSidebarViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var isCreatingItem by remember { mutableStateOf(false) }
    var creatingItemType by remember { mutableStateOf(FileType.FILE) }


    val state by viewModel.uiState.collectAsStateWithLifecycle()

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


    Column (
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
            onAddNoteClick = {
                isCreatingItem = true
                creatingItemType = FileType.FILE
                             },
            onAddFolderClick = {
                isCreatingItem = true
                creatingItemType = FileType.DIRECTORY
                               },
        )

        Spacer(modifier = Modifier.height(16.dp))


        //display the folders content
        if (state.fileTree != null) {
            val visibleItems = remember(state.fileTree, state.expandedFolders) {
                viewModel.getVisibleNodes(state.fileTree!!, state.expandedFolders)
            }
            SidebarFileTree(
                visibleItems = visibleItems,
                rootPath = state.fileTree!!.path,
                onFolderToggle = { viewModel.toggleFolder(it) },
                onFileOpen = { viewModel.setActiveNote(it) },
                isCreatingItem = isCreatingItem,
                creatingItemType = creatingItemType,
                onItemSubmit = { name ->
                    isCreatingItem = false
                    if (creatingItemType == FileType.FILE) {
                        viewModel.addNote(name)
                    } else {
                        viewModel.addFolder(name)
                    }
                },
                onItemCreateCanceled = { isCreatingItem = false },
                onMoveItem = { sourcePath, targetPath ->
                    viewModel.moveItem(sourcePath, targetPath)
                }
            )
        }
    }
}