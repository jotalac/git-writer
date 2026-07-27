package dev.jotalac.feature.editor_sidebar.ui.components.file_tree

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppVerticalScrollbar
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.empty_notebook_label
import org.jetbrains.compose.resources.stringResource

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

@Composable
fun FileTree(
    visibleItems: List<FlatFileNode>,
    rootPath: String,
    onFolderToggle: (String) -> Unit,
    onFileOpen: (String) -> Unit,
    isCreatingItem: Boolean = false,
    creatingItemType: FileType = FileType.FILE,
    onItemSubmit: (String) -> Unit,
    onItemCreateCanceled: () -> Unit,
    onMoveItem: (String, String) -> Unit,
    ) {
    val listState = rememberLazyListState()
    val dragDropState = remember { DragDropState() }
    var treeGlobalPosition by remember { mutableStateOf(Offset.Zero) }
    var treeSize by remember { mutableStateOf(IntSize.Zero) }
    
    val currentDropTarget = computeCurrentDropTarget(
        isDragging = dragDropState.isDragging,
        dragPosition = dragDropState.dragPosition,
        treeGlobalPosition = treeGlobalPosition,
        treeSize = treeSize,
        visibleItemsInfo = listState.layoutInfo.visibleItemsInfo,
        visibleItems = visibleItems,
        rootPath = rootPath
    )
    
    dragDropState.dropTargetResolver = { currentDropTarget }
    
    val isRootTarget = dragDropState.isDragging && currentDropTarget == rootPath

    val rootBorder = if (isRootTarget) Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(0.5f), RoundedCornerShape(8.dp)) else Modifier
    val highlightFill = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    val highlightStroke = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    CompositionLocalProvider(LocalDragDropState provides dragDropState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .then(rootBorder)
                .background(if (isRootTarget) highlightFill else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(8.dp)
                .onGloballyPositioned { 
                    treeGlobalPosition = it.boundsInWindow().topLeft 
                    treeSize = it.size
                }
                .padding(bottom = 20.dp)
        ) {
            if (visibleItems.isEmpty()) {
                Text(
                    text = stringResource(Res.string.empty_notebook_label),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize().drawBehind {
                        val target = currentDropTarget
                        if (target != null && target != rootPath) {
                            val itemsInfo = listState.layoutInfo.visibleItemsInfo
                            val matchingItems = itemsInfo.filter { itemInfo ->
                                val path = itemInfo.key as? String ?: return@filter false
                                val separator = if (path.contains("\\")) "\\" else "/"
                                path == target || path.startsWith(target + separator)
                            }
                            
                            if (matchingItems.isNotEmpty()) {
                                val firstItem = matchingItems.first()
                                val lastItem = matchingItems.last()
                                val top = firstItem.offset.toFloat()
                                val bottom = (lastItem.offset + lastItem.size).toFloat()
                                
                                drawRoundRect(
                                    color = highlightFill,
                                    topLeft = Offset(x = 0f, y = top),
                                    size = Size(width = size.width, height = bottom - top),
                                    cornerRadius = CornerRadius(8.dp.toPx())
                                )
                                drawRoundRect(
                                    color = highlightStroke,
                                    topLeft = Offset(x = 0f, y = top),
                                    size = Size(width = size.width, height = bottom - top),
                                    cornerRadius = CornerRadius(8.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    },
                    state = listState
                ) {
                    if (isCreatingItem) {
                        item {
                            NewItemRow(
                                onSubmit = onItemSubmit,
                                onCancel = onItemCreateCanceled,
                                itemType = creatingItemType,
                                modifier = Modifier.animateItem(),
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
                            },
                            onMoveItem = onMoveItem
                        )
                    }
                }
                AppVerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    listState = listState
                )
            }
            
            DragShadow(
                dragDropState = dragDropState,
                treeGlobalPosition = treeGlobalPosition,
                treeSize = treeSize
            )
        }
    }
}

private fun computeCurrentDropTarget(
    isDragging: Boolean,
    dragPosition: Offset,
    treeGlobalPosition: Offset,
    treeSize: IntSize,
    visibleItemsInfo: List<LazyListItemInfo>,
    visibleItems: List<FlatFileNode>,
    rootPath: String
): String? {
    if (!isDragging) return null
    
    val localDragY = dragPosition.y - treeGlobalPosition.y
    val localDragX = dragPosition.x - treeGlobalPosition.x
    val isInsideTree = localDragX >= 0 && localDragX <= treeSize.width && localDragY >= 0 && localDragY <= treeSize.height
    
    if (!isInsideTree) return null

    val hoveredRowPath = visibleItemsInfo.firstOrNull {
        localDragY >= it.offset && localDragY <= (it.offset + it.size)
    }?.key as? String

    val hoveredNode = visibleItems.find { it.node.path == hoveredRowPath } ?: return rootPath
    
    val separator = if (hoveredNode.node.path.contains("\\")) "\\" else "/"
    return if (hoveredNode.node is FileNode.Directory) {
        hoveredNode.node.path
    } else {
        hoveredNode.node.path.substringBeforeLast(separator)
    }
}

@Composable
private fun DragShadow(
    dragDropState: DragDropState,
    treeGlobalPosition: Offset,
    treeSize: IntSize,
) {
    if (!dragDropState.isDragging || dragDropState.draggedNode == null) return
    
    val node = dragDropState.draggedNode!!
    val localOffset = dragDropState.dragPosition - treeGlobalPosition
    val shadowX = localOffset.x - dragDropState.dragOffsetWithinNode.x
    val shadowY = localOffset.y - dragDropState.dragOffsetWithinNode.y
    val shadowWidth = with(LocalDensity.current) { (treeSize.width).toDp() - 16.dp }
    
    FileTreeRow(
        flatNode = node,
        modifier = Modifier
            .width(shadowWidth)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeWithLayer(
                        x = shadowX.toInt(),
                        y = shadowY.toInt(),
                        layerBlock = {
                            alpha = 0.8f
                            shadowElevation = 8f
                        }
                    )
                }
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
        onClick = {},
        onMoveItem = { _, _ -> }
    )
}