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
import dev.jotalac.feature.editor.ui.components.EditorSidebar
import dev.jotalac.feature.editor.ui.components.MarkdownEditor
import dev.jotalac.feature.editor.ui.components.SidebarContent
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.closed_sidebar
import git_writer.shared.generated.resources.opened_sidebar
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(viewModel: EditorViewModel = EditorViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blocks = viewModel.markdownBlocks

    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }

    EditorScreenContent(
        snackbarHostState = snackbarHostState,
        markdownBlocks = blocks,
        filename = state.filename,
        isLoading = state.isLoading,
        onAction = viewModel::onAction
    )
}

@Composable
fun EditorScreenContent(
    snackbarHostState: SnackbarHostState,
    markdownBlocks: List<String>,
    filename: String?,
    isLoading: Boolean,
    onAction: (EditorAction) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactScreen = maxWidth < 600.dp

        if (isCompactScreen) {
            CompactEditorLayout(
                snackbarHostState = snackbarHostState,
                markdownBlocks = markdownBlocks,
                filename = filename,
                isLoading = isLoading,
                onAction = onAction
            )
        } else {
            ExpandedEditorLayout(
                snackbarHostState = snackbarHostState,
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
    snackbarHostState: SnackbarHostState,
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
            snackbarHostState = snackbarHostState,
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
    snackbarHostState: SnackbarHostState,
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
            snackbarHostState = snackbarHostState,
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
    snackbarHostState: SnackbarHostState,
    filename: String?,
    isSidebarOpen: Boolean,
    onToggleSidebar: () -> Unit,
    isLoading: Boolean,
    markdownBlocks: List<String>,
    onAction: (EditorAction) -> Unit,
) {
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
            snackbarHostState = SnackbarHostState()
        )
    }
}