package dev.jotalac.feature.editor.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jotalac.core.ui.components.CustomScaffold
import dev.jotalac.core.ui.components.TopAppBarIcon
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.core.ui.window.TitleBar
import dev.jotalac.core.ui.window.WindowTitle
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.editor.ui.components.EditorTabsRow
import dev.jotalac.feature.editor.ui.components.MarkdownEditor
import dev.jotalac.feature.editor.ui.components.NoFileOpenedMessage
import dev.jotalac.feature.editor.ui.components.SyncFloatingButton
import dev.jotalac.feature.editor.ui.components.editor_find.MarkdownFindState
import dev.jotalac.feature.editor_sidebar.ui.EditorSidebar
import dev.jotalac.feature.editor_sidebar.ui.SidebarContent
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.git_sync.ui.GitConflictResolveDialog
import git_writer.shared.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditorScreen(
    openSettingsOnMobile: () -> Unit,
    viewModel: EditorViewModel = koinViewModel()
) {
    val blocks = viewModel.markdownBlocks
    val editorUiState = viewModel.uiState.collectAsStateWithLifecycle()
    val state = editorUiState.value

    // Keep the OS window/taskbar title in sync with the active document (desktop only).
    WindowTitle(state.activeFilename)

    val rootFocusRequester = remember { FocusRequester() }

    // when there is no note open - focus the root
    LaunchedEffect(state.activeNotePath, state.isImage) {
        val hasEditor = state.activeNotePath != null && !state.isImage
        if (!hasEditor) {
            try {
                rootFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

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

    Box(
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

                    Key.N -> {
                        if (state.activeNotePath == null) {
                            viewModel.onAction(EditorAction.NewNote)
                            true
                        } else false
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
            .focusRequester(rootFocusRequester)
            .focusable()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
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
                    uiState = editorUiState,
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
                    uiState = editorUiState,
                    onTabClick = viewModel::openTab,
                    onTabClose = viewModel::closeTab,
                    onNewTab = viewModel::addNewTab,
                )
            }
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
    uiState: State<EditorScreenState>,
    onTabClick: (Long) -> Unit,
    onTabClose: (Long) -> Unit,
    onNewTab: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDrawerOpen = remember { derivedStateOf { drawerState.isOpen } }

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
            isSidebarOpen = isDrawerOpen,
            onToggleSidebar = {
                scope.launch {
                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }
            },
            isLoading = isLoading,
            markdownBlocks = markdownBlocks,
            onAction = onAction,
            gitSyncStatus = gitSyncStatus,
            uiState = uiState,
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
    uiState: State<EditorScreenState>,
    onTabClick: (Long) -> Unit,
    onTabClose: (Long) -> Unit,
    onNewTab: () -> Unit,
) {
    val sidebarVisibility = remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        EditorSidebar(
            isVisible = sidebarVisibility.value,
            openSettingsOnMobile = openSettingsOnMobile,
        )

        MainEditorScaffold(
            modifier = Modifier.weight(1f),
            filename = filename,
            activeNotePath = activeNotePath,
            isImage = isImage,
            isSidebarOpen = sidebarVisibility,
            onToggleSidebar = {
                sidebarVisibility.value = !sidebarVisibility.value
            },
            isLoading = isLoading,
            markdownBlocks = markdownBlocks,
            onAction = onAction,
            gitSyncStatus = gitSyncStatus,
            uiState = uiState,
            onTabClick = onTabClick,
            onTabClose = onTabClose,
            onNewTab = onNewTab,
        )
    }
}

@Composable
private fun MainEditorScaffold(
    modifier: Modifier = Modifier,
    filename: String?,
    activeNotePath: String?,
    isImage: Boolean,
    isSidebarOpen: State<Boolean>,
    onToggleSidebar: () -> Unit,
    isLoading: Boolean,
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit,
    gitSyncStatus: GitSyncStatus,
    uiState: State<EditorScreenState>,
    onTabClick: (Long) -> Unit,
    onTabClose: (Long) -> Unit,
    onNewTab: () -> Unit,
) {

    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val snackbarManager = koinInject<SnackbarManager>()

    // shared between the find FAB (mobile) and the editor content; recreated per note so the
    // find bar doesn't carry over between tabs
    val findState = key(activeNotePath) { remember { MarkdownFindState() } }

    LaunchedEffect(Unit) {
        snackbarManager.messages.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }


    CustomScaffold(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        topAppBar = {
            TitleBar(
                leading = {
                    TopAppBarIcon(
                        onClick = onToggleSidebar,
                        icon = if (isSidebarOpen.value) Res.drawable.opened_sidebar else Res.drawable.closed_sidebar,
                        contentDescription = stringResource(Res.string.toggle_side_bar_desc),
                    )
                },
                title = {
                    val editorState = uiState.value
                    EditorTabsRow(
                        tabs = editorState.openedTabs,
                        activeTabId = editorState.activeTabId,
                        onItemClick = { onTabClick(it.id) },
                        onItemClose = { onTabClose(it.id) },
                        onNewTab = onNewTab,
                    )
                },
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!isDesktopPlatform) {
                    FloatingActionButton(onClick = { findState.open() }) {
                        Icon(
                            painter = painterResource(Res.drawable.search),
                            contentDescription = stringResource(Res.string.find_button),
                        )
                    }
                }

                SyncFloatingButton(
                    onClick = { onAction(EditorAction.SyncNotes) },
                    gitSyncStatus = gitSyncStatus
                )
            }
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
                onAction = onAction,
                findState = findState,
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
    onAction: (EditorAction) -> Unit,
    findState: MarkdownFindState,
) {
    // remember scroll position per note, so switching tabs doesn't reset it
    val scrollPositions = remember { mutableMapOf<String, Int>() }

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
            contentScale = ContentScale.Inside,

        )
    } else {
        val notePath = requireNotNull(activeNotePath)
        key(notePath) { // for undo/redo logic to reset
            MarkdownEditor(
                markdownBlocks = markdownBlocks,
                onAction = onAction,
                modifier = Modifier.fillMaxSize(),
                initialScroll = scrollPositions[notePath] ?: 0,
                onScrollOffsetChanged = { offset -> scrollPositions[notePath] = offset },
                findState = findState,
            )
        }
    }
}
