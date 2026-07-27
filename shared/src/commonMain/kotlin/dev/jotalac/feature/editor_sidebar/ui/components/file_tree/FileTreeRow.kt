package dev.jotalac.feature.editor_sidebar.ui.components.file_tree

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.arrow_right
import org.jetbrains.compose.resources.painterResource

@Composable
fun FileTreeRow(
    flatNode: FlatFileNode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoveItem: (String, String) -> Unit,
) {
    val indentPadding = (flatNode.depth * 12).dp
    val rotation by animateFloatAsState(targetValue = if (flatNode.isExpanded) 90f else 0f)

    val dragDropState = LocalDragDropState.current
    val isDragged = dragDropState.isDragging && dragDropState.draggedNode?.node?.path == flatNode.node.path

    val dragModifier = Modifier.dragSource(
        node = flatNode,
        dragDropState = dragDropState,
        onDragEnd = { dropTarget ->
            if (dropTarget != null) {
                onMoveItem(flatNode.node.path, dropTarget)
            }
        }
    )

    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    Box {
        Row(
            modifier = modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val position = event.changes.first().position
                                menuOffset = with(density) {
                                    DpOffset(position.x.toDp(), (position.y - 10).toDp() )
                                }
                                showMenu = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .then(dragModifier)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(6.dp))
                .then(if (showMenu) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest) else Modifier)
                .clickable(onClick = onClick)
                .padding(start = indentPadding + 4.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
                .alpha(if (isDragged) 0.5f else 1f),
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
                    val filename = if (lastDotIndex != -1) flatNode.node.name.substring(0, lastDotIndex) else flatNode.node.name
                    val fileExtension = if (lastDotIndex != -1) flatNode.node.name.substring(lastDotIndex + 1) else ""

                    Text(
                        text = filename,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (fileExtension.isNotEmpty() && fileExtension.lowercase() != "md") {
                        Text(
                            text = fileExtension.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        ItemContextMenu(
            showMenu = showMenu,
            menuOffset = menuOffset,
            onDismissRequest = { showMenu = false },
            onRename = {},
            onDelete = {},
            itemType = if (flatNode.node is FileNode.File) FileType.FILE else FileType.DIRECTORY,
        )

    }
}