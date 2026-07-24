package dev.jotalac.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.ui.components.CustomScaffold
import dev.jotalac.core.ui.components.TopAppBarIcon
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.editor.ui.components.MarkdownEditor
import dev.jotalac.feature.editor.ui.components.NoFileOpenedMessage
import dev.jotalac.feature.editor_sidebar.ui.EditorSidebar
import dev.jotalac.feature.editor_sidebar.ui.SidebarContent
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.closed_sidebar
import git_writer.shared.generated.resources.no_file_loaded_msg
import git_writer.shared.generated.resources.opened_book
import git_writer.shared.generated.resources.opened_sidebar
import git_writer.shared.generated.resources.x_icon
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditorScreen(viewModel: EditorViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blocks = viewModel.markdownBlocks

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactScreen = maxWidth < 600.dp

        if (isCompactScreen) {
            CompactEditorLayout(
                markdownBlocks = blocks,
                filename = state.activeFilename,
                isLoading = state.isLoading,
                onNoteClose = viewModel::closeActiveNote,
                onAction = viewModel::onAction
            )
        } else {
            ExpandedEditorLayout(
                markdownBlocks = blocks,
                filename = state.activeFilename,
                isLoading = state.isLoading,
                onNoteClose = viewModel::closeActiveNote,
                onAction = viewModel::onAction
            )
        }
    }
}

@Composable
private fun CompactEditorLayout(
    markdownBlocks: List<String>,
    filename: String?,
    isLoading: Boolean,
    onNoteClose: () -> Unit,
    onAction: (EditorAction) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                SidebarContent()
            }
        }
    ) {
        MainEditorScaffold(
            filename = filename,
            isSidebarOpen = drawerState.isOpen,
            onToggleSidebar = {
                scope.launch {
                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }
            },
            onNoteClose = onNoteClose,
            isLoading = isLoading,
            markdownBlocks = markdownBlocks,
            onAction = onAction,
        )
    }
}

@Composable
private fun ExpandedEditorLayout(
    markdownBlocks: List<String>,
    filename: String?,
    isLoading: Boolean,
    onNoteClose: () -> Unit,
    onAction: (EditorAction) -> Unit
) {
    var isSidebarVisible by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        EditorSidebar(isVisible = isSidebarVisible)
        
        MainEditorScaffold(
            modifier = Modifier.weight(1f),
            filename = filename,
            isSidebarOpen = isSidebarVisible,
            onToggleSidebar = {
                isSidebarVisible = !isSidebarVisible
            },
            onNoteClose = onNoteClose,
            isLoading = isLoading,
            markdownBlocks = markdownBlocks,
            onAction = onAction,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorScaffold(
    modifier: Modifier = Modifier,
    filename: String?,
    isSidebarOpen: Boolean,
    onToggleSidebar: () -> Unit,
    onNoteClose: () -> Unit,
    isLoading: Boolean,
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit,
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
                modifier = Modifier.heightIn(max = 70.dp),
                title = {
                    if (filename != null) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                filename,
                                modifier = Modifier
                                    .padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            IconButton(
                                onClick = { onNoteClose() },
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.x_icon),
                                    contentDescription = "close icon",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    TopAppBarIcon(
                        onClick = onToggleSidebar,
                        icon = if (isSidebarOpen) Res.drawable.opened_sidebar else Res.drawable.closed_sidebar,
                        contentDescription = "Toggle sidebar visibility",
                    )
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading) {
                // how loading indicator when file is loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(150.dp))
                }
            } else if (filename == null) {
                NoFileOpenedMessage()
            } else {
                // show editor if file is opened
                MarkdownEditor(
                    markdownBlocks = markdownBlocks,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
