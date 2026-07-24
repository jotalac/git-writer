package dev.jotalac.feature.editor_sidebar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.arrow_down
import git_writer.shared.generated.resources.arrow_right
import org.jetbrains.compose.resources.painterResource

@Composable
fun SidebarFileTree(
    visibleItems: List<FlatFileNode>,
    onFolderToggle: (String) -> Unit,
    onFileOpen: (String) -> Unit,

    ) {
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp)

    ) {
        if (visibleItems.isEmpty()) {
            Text(
                text = "Notebook is empty",
    //            modifier = Modifier.(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(
                    items = visibleItems,
                    key = { flatNode -> flatNode.node.path }
                ) { flatNode ->
                    FileTreeRow(
                        flatNode = flatNode,
                        onClick = {
                            when (val node = flatNode.node) {
                                is FileNode.Directory -> { onFolderToggle(node.path) }
                                is FileNode.File -> { onFileOpen(node.path) }
                            }
                        }
                    )
                }
            }
            AppVerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                listState = listState
            )
        }
    }
}

@Composable
fun FileTreeRow(
    flatNode: FlatFileNode,
    onClick: () -> Unit
) {
    val indentPadding = (flatNode.depth * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = indentPadding, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (flatNode.node) {
            is FileNode.Directory -> {
                Icon(
                    // Replace with a dynamic open/closed folder icon if desired
                    painter = painterResource(
                        if (flatNode.isExpanded) Res.drawable.arrow_down else Res.drawable.arrow_right
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = flatNode.node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is FileNode.File -> {
                Spacer(modifier = Modifier.width(24.dp)) // Aligns text with folder icons
                Text(
                    text = flatNode.node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}