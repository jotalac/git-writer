package dev.jotalac.feature.sidebar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.utils.buildClipEntry
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.feature.notebooks_management.ui.list_notebooks.NotebookListViewModel
import dev.jotalac.feature.sidebar.ui.components.NoNotebookOpen
import dev.jotalac.feature.sidebar.ui.components.NotebookSwitcher
import dev.jotalac.feature.sidebar.ui.components.SidebarButtonWithTooltip
import dev.jotalac.feature.sidebar.ui.components.SidebarNavItem
import dev.jotalac.feature.sidebar.ui.components.SidebarPrimaryActions
import dev.jotalac.feature.sidebar.ui.components.SidebarSectionHeader
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.collapse_all
import git_writer.shared.generated.resources.collapse_folders
import git_writer.shared.generated.resources.expand_all
import git_writer.shared.generated.resources.expand_folders
import git_writer.shared.generated.resources.settings
import git_writer.shared.generated.resources.settings_title
import git_writer.shared.generated.resources.sidebar_files_header
import org.jetbrains.compose.resources.stringResource
import dev.jotalac.feature.sidebar.ui.components.file_tree.FileTree
import dev.jotalac.feature.notebooks_management.ui.create_notebook.CreateNotebookDialog
import dev.jotalac.feature.notebooks_management.ui.edit_notebook.EditNotebookDialog
import dev.jotalac.feature.notebooks_management.ui.list_notebooks.NotebookListDialog
import dev.jotalac.feature.pdf_export.PdfExportDialog
import dev.jotalac.feature.settings.ui.SettingsDialog
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun SidebarContent(
    viewModel: EditorSidebarViewModel = koinViewModel(),
    notebookListViewModel: NotebookListViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    onSidebarClose: () -> Unit = {},
    onOpenSettingsOnMobile: () -> Unit,
    onOpenEditNotebookOnMobile: (notebookId: Long) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var showEditNotebookDialog by remember { mutableStateOf(false) }
    var showDesktopSettingsDialog by remember { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notebooks by notebookListViewModel.uiState.collectAsStateWithLifecycle()

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

    state.pdfExport?.let { request ->
        PdfExportDialog(request, onFinished = { viewModel.onAction(SidebarAction.PdfExportFinished(it)) })
    }

    if (showDesktopSettingsDialog) {
        SettingsDialog(onDismiss = { showDesktopSettingsDialog = false })
    }


    val openSettings: () -> Unit = {
        if (isDesktopPlatform) {
            showDesktopSettingsDialog = true
        } else {
            onSidebarClose()
            onOpenSettingsOnMobile()
        }
    }
    val openEditNotebook: () -> Unit = {
        if (isDesktopPlatform) {
            showEditNotebookDialog = true
        } else {
            state.activeNotebook?.let { notebook ->
                onSidebarClose()
                onOpenEditNotebookOnMobile(notebook.id)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .onPreviewKeyEvent { event ->
                // rename active note with f2
                if (state.activeNotePath == null || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                when (event.key) {
                    Key.F2 -> {
                        viewModel.onAction(SidebarAction.SetRenameItem(state.activeNotePath))
                        true
                    }

                    Key.Delete -> {
                        viewModel.onAction(SidebarAction.DeleteItem(state.activeNotePath!!))
                        true
                    }

                    Key.D -> {
                        if (event.isCtrlPressed || event.isMetaPressed) {
                            viewModel.onAction(SidebarAction.DuplicateNote(state.activeNotePath!!))
                            true
                        } else false
                    }

                    else -> false
                }
            }
    ) {
        NotebookSwitcher(
            activeNotebook = state.activeNotebook,
            notebooks = notebooks.notebooks,
            onSwitch = { notebook ->
                notebookListViewModel.onEvent(NotebookListViewModel.NotebookListEvent.OpenNotebook(notebook.id) {})
            },
            onCreate = { showCreateDialog = true },
            onManage = { showListDialog = true },
            onEditActive = openEditNotebook,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.activeNotebook == null) {
            NoNotebookOpen(
                hasNotebooks = notebooks.notebooks.isNotEmpty(),
                onCreate = { showCreateDialog = true },
                onOpen = { showListDialog = true },
                modifier = Modifier.weight(1f),
            )
        } else {
            SidebarPrimaryActions(
                onNewNote = { viewModel.onAction(SidebarAction.AddNote()) },
                onNewFolder = { viewModel.onAction(SidebarAction.AddFolder()) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            val anyFolderExpanded = state.expandedFolders.isNotEmpty()
            SidebarSectionHeader(title = stringResource(Res.string.sidebar_files_header)) {
                SidebarButtonWithTooltip(
                    onClick = viewModel::toggleFolderCollapse,
                    icon = if (anyFolderExpanded) Res.drawable.collapse_all else Res.drawable.expand_all,
                    contentDescription = stringResource(
                        if (anyFolderExpanded) Res.string.collapse_folders else Res.string.expand_folders
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = MaterialTheme.dimensions.iconMedium,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            //display the folders content
            Box(modifier = Modifier.weight(1f)) {
                state.fileTree?.let { fileTree ->
                    val visibleItems = remember(fileTree, state.expandedFolders) {
                        viewModel.getVisibleNodes(fileTree, state.expandedFolders)
                    }

                    FileTree(
                        visibleItems = visibleItems,
                        activeNotePath = state.activeNotePath,
                        rootPath = fileTree.path,
                        onFolderToggle = { viewModel.toggleFolder(it) },
                        itemToRename = state.itemToRename,
                        onAction = { action ->
                            // handle the copy item with clipboard manager
                            if (action is SidebarAction.CopyItemPath) {
                                scope.launch {
                                    clipboardManager.setClipEntry(buildClipEntry(action.path))
                                }
                            }
                            // close the sidebar on note open on mobile
                            if (action is SidebarAction.OpenNote) onSidebarClose()
                            viewModel.onAction(action)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SidebarNavItem(
            icon = Res.drawable.settings,
            label = stringResource(Res.string.settings_title),
            onClick = openSettings,
        )
    }
}