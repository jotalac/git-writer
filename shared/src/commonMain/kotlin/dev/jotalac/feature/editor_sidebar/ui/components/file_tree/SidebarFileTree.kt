package dev.jotalac.feature.editor_sidebar.ui.components.file_tree

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.input.ImeAction
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.arrow_right
import git_writer.shared.generated.resources.empty_notebook_label
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SidebarFileTree(
    visibleItems: List<FlatFileNode>,
    onFolderToggle: (String) -> Unit,
    onFileOpen: (String) -> Unit,
    isCreatingNote: Boolean = false,
    onNoteCreated: (String) -> Unit = {},
    onNoteCreateCanceled: () -> Unit = {},
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
                text = stringResource(Res.string.empty_notebook_label),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                if (isCreatingNote) {
                    item {
                        NewFileRow(
                            onSubmit = onNoteCreated,
                            onCancel = onNoteCreateCanceled,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                items(
                    items = visibleItems,
                    key = { flatNode -> flatNode.node.path },
                    contentType = { flatNode -> if (flatNode.node is FileNode.Directory) 1 else 0 }
                ) { flatNode ->
                    FileTreeRow(
                        flatNode = flatNode,
                        modifier = Modifier.animateItem(),
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val indentPadding = (flatNode.depth * 12).dp
    val rotation by animateFloatAsState(targetValue = if (flatNode.isExpanded) 90f else 0f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(start = indentPadding + 4.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (flatNode.node) {
            is FileNode.Directory -> {
                Icon(
                    painter = painterResource(Res.drawable.arrow_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = flatNode.node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is FileNode.File -> {
                Spacer(modifier = Modifier.width(20.dp))

                val lastDotIndex = flatNode.node.name.lastIndexOf('.')
                val filename = flatNode.node.name.substring(0, lastDotIndex)
                val fileExtension = flatNode.node.name.substring(lastDotIndex + 1)

                Text(
                    text = filename,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // display file type if it is not markdown file
                if (fileExtension.lowercase() != "md") {
                    Text(
                        text = fileExtension.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                }
            }
        }
    }
}