package dev.jotalac.feature.editor.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jotalac.core.ui.components.CustomScaffold
import dev.jotalac.core.ui.components.TopAppBarIcon
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.SnackbarManager
import dev.jotalac.feature.editor.ui.components.MarkdownEditor
import dev.jotalac.feature.editor_sidebar.ui.EditorSidebar
import dev.jotalac.feature.editor_sidebar.ui.SidebarContent
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.closed_sidebar
import git_writer.shared.generated.resources.opened_sidebar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditorScreen(viewModel: EditorViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blocks = viewModel.markdownBlocks

    EditorScreenContent(
        markdownBlocks = blocks,
        filename = state.filename,
        isLoading = state.isLoading,
        onAction = viewModel::onAction
    )
}

@Composable
fun EditorScreenContent(
    markdownBlocks: List<String>,
    filename: String?,
    isLoading: Boolean,
    onAction: (EditorAction) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactScreen = maxWidth < 600.dp

        if (isCompactScreen) {
            CompactEditorLayout(
                markdownBlocks = markdownBlocks,
                filename = filename,
                isLoading = isLoading,
                onAction = onAction
            )
        } else {
            ExpandedEditorLayout(
                markdownBlocks = markdownBlocks,
                filename = filename,
                isLoading = isLoading,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun CompactEditorLayout(
    markdownBlocks: List<String>,
    filename: String?,
    isLoading: Boolean,
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
                    Text(filename ?: "unknown file", modifier = Modifier.padding(start = 16.dp))
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
            if (!isLoading) {
                MarkdownEditor(
                    markdownBlocks = markdownBlocks,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LoadingIndicator()
            }
        }
    }
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun EditorScreenPreview() {
    AppTheme {
        EditorScreenContent(
            markdownBlocks = listOf("sdf", "sdf"),
            filename = null,
            onAction = {},
            isLoading = false,
        )
    }
}