package dev.jotalac.feature.editor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jotalac.core.ui.components.CustomScaffold
import dev.jotalac.core.ui.components.TopAppBarIcon
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.editor.domain.EditorTabItem
import dev.jotalac.feature.editor.ui.components.EditorTabsRow
import dev.jotalac.feature.editor.ui.components.MarkdownEditor
import dev.jotalac.feature.editor.ui.components.NoFileOpenedMessage
import dev.jotalac.feature.editor.ui.components.SyncFloatingButton
import dev.jotalac.feature.editor_sidebar.ui.EditorSidebar
import dev.jotalac.feature.editor_sidebar.ui.SidebarContent
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.git_sync.ui.GitConflictResolveDialog
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.closed_sidebar
import git_writer.shared.generated.resources.opened_sidebar
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditorScreen(
    openSettingsOnMobile: () -> Unit,
    viewModel: EditorViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blocks = viewModel.markdownBlocks

    if (state.conflictedFiles.isNotEmpty()) {
        GitConflictResolveDialog(
            conflictedFileNames = state.conflictedFiles,
            onKeepLocal = { fileName ->
                viewModel.onAction(EditorAction.ResolveSingleConflict(fileName, keepLocalChanges = true))
            },
            onKeepRemote = { fileName ->
                viewModel.onAction(EditorAction.ResolveSingleConflict(fileName, keepLocalChanges = false))
            },
            onKeepAllLocal = {
                viewModel.onAction(EditorAction.ResolveAllConflicts(keepLocalChanges = true))
            },
            onKeepAllRemote = {
                viewModel.onAction(EditorAction.ResolveAllConflicts(keepLocalChanges = false))
            },
            onDismiss = {
                viewModel.onAction(EditorAction.AbortConflictResolve)
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                // handle global editor shortcuts - later refactor all the key shortcuts to separate file
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                val isShortcut = event.isCtrlPressed || event.isMetaPressed
                if (!isShortcut) return@onPreviewKeyEvent false

                when (event.key) {
                    Key.W -> {
                        viewModel.onAction(EditorAction.CloseActiveTab)
                        true
                    }

                    Key.T -> {
                        viewModel.onAction(EditorAction.NewTab)
                        true
                    }

                    Key.Tab -> {
                        viewModel.onAction(
                            if (event.isShiftPressed) EditorAction.PreviousTab else EditorAction.NextTab
                        )
                        true
                    }

                    else -> false
                }
            }
    ) {
        val isCompactScreen = maxWidth < 600.dp

        if (isCompactScreen) {
            CompactEditorLayout(
                markdownBlocks = blocks,
                filename = state.activeFilename,
                activeNotePath = state.activeNotePath,
                isImage = state.isImage,
                isLoading = state.isLoading,
                onAction = viewModel::onAction,
                gitSyncStatus = state.gitSyncStatus,
                openSettingsOnMobile = openSettingsOnMobile,
                openedTabs = state.openedTabs,
                activeTabId = state.activeTabId,
                onTabClick = viewModel::openTab,
                onTabClose = viewModel::closeTab,
                onNewTab = viewModel::addNewTab,
            )
        } else {
            ExpandedEditorLayout(
                markdownBlocks = blocks,
                filename = state.activeFilename,
                activeNotePath = state.activeNotePath,
                isImage = state.isImage,
                isLoading = state.isLoading,
                onAction = viewModel::onAction,
                gitSyncStatus = state.gitSyncStatus,
                openSettingsOnMobile = openSettingsOnMobile,
                openedTabs = state.openedTabs,
                activeTabId = state.activeTabId,
                onTabClick = viewModel::openTab,
                onTabClose = viewModel::closeTab,
                onNewTab = viewModel::addNewTab,
            )
        }
    }
}

@Composable
private fun CompactEditorLayout(
    markdownBlocks: List<String>,
    filename: String?,
    activeNotePath: String?,
    isImage: Boolean,
    isLoading: Boolean,
    onAction: (EditorAction) -> Unit,
    gitSyncStatus: GitSyncStatus,
    openSettingsOnMobile: () -> Unit,
    openedTabs: List<EditorTabItem>,
    activeTabId: Long,
    onTabClick: (Long) -> Unit,
    onTabClose: (Long) -> Unit,
    onNewTab: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(MaterialTheme.dimensions.navDrawerWidth)) {
                SidebarContent(
                    onSidebarClose = { scope.launch { drawerState.close() } },
                    onOpenSettingsOnMobile = openSettingsOnMobile
                )
            }
        }
    ) {
        MainEditorScaffold(
            filename = filename,
            activeNotePath = activeNotePath,
            isImage = isImage,
            isSidebarOpen = drawerState.isOpen,
            onToggleSidebar = {
                scope.launch {
                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }
            },
            isLoading = isLoading,
            markdownBlocks = markdownBlocks,
            onAction = onAction,
            gitSyncStatus = gitSyncStatus,
            openedTabs = openedTabs,
            activeTabId = activeTabId,
            onTabClick = onTabClick,
            onTabClose = onTabClose,
            onNewTab = onNewTab,
        )
    }
}

@Composable
private fun ExpandedEditorLayout(
    markdownBlocks: List<String>,
    filename: String?,
    activeNotePath: String?,
    isImage: Boolean,
    isLoading: Boolean,
    onAction: (EditorAction) -> Unit,
    gitSyncStatus: GitSyncStatus,
    openSettingsOnMobile: () -> Unit = {},
    openedTabs: List<EditorTabItem>,
    activeTabId: Long,
    onTabClick: (Long) -> Unit,
    onTabClose: (Long) -> Unit,
    onNewTab: () -> Unit,
) {
    var isSidebarVisible by remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        EditorSidebar(
            isVisible = isSidebarVisible,
            openSettingsOnMobile = openSettingsOnMobile,
        )

        MainEditorScaffold(
            modifier = Modifier.weight(1f),
            filename = filename,
            activeNotePath = activeNotePath,
            isImage = isImage,
            isSidebarOpen = isSidebarVisible,
            onToggleSidebar = {
                isSidebarVisible = !isSidebarVisible
            },
            isLoading = isLoading,
            markdownBlocks = markdownBlocks,
            onAction = onAction,
            gitSyncStatus = gitSyncStatus,
            openedTabs = openedTabs,
            activeTabId = activeTabId,
            onTabClick = onTabClick,
            onTabClose = onTabClose,
            onNewTab = onNewTab,
        )
    }
}

@Composable
fun MainEditorScaffold(
    modifier: Modifier = Modifier,
    filename: String?,
    activeNotePath: String?,
    isImage: Boolean,
    isSidebarOpen: Boolean,
    onToggleSidebar: () -> Unit,
    isLoading: Boolean,
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit,
    gitSyncStatus: GitSyncStatus,
    openedTabs: List<EditorTabItem>,
    activeTabId: Long,
    onTabClick: (Long) -> Unit,
    onTabClose: (Long) -> Unit,
    onNewTab: () -> Unit,
) {

    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val snackbarManager = koinInject<SnackbarManager>()

    LaunchedEffect(Unit) {
        snackbarManager.messages.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }


    CustomScaffold(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        topAppBar = {
            TopAppBar(
                title = {
                    EditorTabsRow(
                        tabs = openedTabs,
                        activeTabId = activeTabId,
                        onItemClick = { onTabClick(it.id) },
                        onItemClose = { onTabClose(it.id) },
                        onNewTab = onNewTab,
                    )
                },
                navigationIcon = {
                    TopAppBarIcon(
                        onClick = onToggleSidebar,
                        icon = if (isSidebarOpen) Res.drawable.opened_sidebar else Res.drawable.closed_sidebar,
                        contentDescription = "Toggle sidebar visibility",
                    )
                },
            )
        },
        floatingActionButton = {
            SyncFloatingButton(
                onClick = { onAction(EditorAction.SyncNotes) },
                gitSyncStatus = gitSyncStatus
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            EditorContent(
                isLoading = isLoading,
                filename = filename,
                isImage = isImage,
                activeNotePath = activeNotePath,
                markdownBlocks = markdownBlocks,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun EditorContent(
    isLoading: Boolean,
    filename: String?,
    isImage: Boolean,
    activeNotePath: String?,
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
        }
    } else if (filename == null) {
        NoFileOpenedMessage()
    } else if (isImage && activeNotePath != null) {
        AsyncImage(
            model = PlatformFile(activeNotePath),
            contentDescription = filename,
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentScale = ContentScale.Inside
        )
    } else {
        key(activeNotePath) { // for undo/redo logic to reset
            MarkdownEditor(
                markdownBlocks = markdownBlocks,
                onAction = onAction,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
