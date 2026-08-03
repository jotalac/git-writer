package dev.jotalac.feature.editor_sidebar.ui.components.file_tree

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.editor_sidebar.domain.FileNode
import dev.jotalac.feature.editor_sidebar.domain.FileType
import dev.jotalac.feature.editor_sidebar.domain.FlatFileNode
import dev.jotalac.feature.editor_sidebar.ui.SidebarAction
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.arrow_right
import git_writer.shared.generated.resources.more_vert
import org.jetbrains.compose.resources.painterResource

@Composable
fun FileTreeRow(
    flatNode: FlatFileNode,
    modifier: Modifier = Modifier,
    isRenaming: Boolean = false,
    onAction: (SidebarAction) -> Unit = {},
    onClick: () -> Unit,
) {
    val dragDropState = LocalDragDropState.current
    val isDragged = dragDropState.isDragging && dragDropState.draggedNode?.node?.path == flatNode.node.path

    val dragModifier = Modifier.dragSource(
        node = flatNode,
        dragDropState = dragDropState,
        onDragEnd = { dropTarget ->
            if (dropTarget != null) {
                onAction(SidebarAction.MoveItem(flatNode.node.path, dropTarget))
            }
        }
    )

    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    val baseName = remember(flatNode.node.name) {
        when (flatNode.node) {
            is FileNode.File -> flatNode.node.name.substringBeforeLast('.')
            is FileNode.Directory -> flatNode.node.name
        }
    }

    Box {
        Row(
            modifier = modifier
                .onRightClick { offset ->
                    menuOffset = offset
                    showMenu = true
                }
                .then(dragModifier)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(6.dp))
                .then(if (showMenu) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest) else Modifier)
                .clickable(
                    enabled = !isRenaming,
                    onClick = onClick
                )
                .padding(
                    start = (MaterialTheme.dimensions.listItemIndentPerLevel * flatNode.depth) + 4.dp,
                    top = MaterialTheme.dimensions.listItemPaddingVertical,
                    bottom = MaterialTheme.dimensions.listItemPaddingVertical,
                    end = 8.dp
                )
                .alpha(if (isDragged) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (flatNode.node) {
                is FileNode.Directory -> {
                    DirectoryContent(
                        node = flatNode.node,
                        isExpanded = flatNode.isExpanded,
                        isRenaming = isRenaming,
                        baseName = baseName,
                        onAction = onAction
                    )
                }

                is FileNode.File -> {
                    FileContent(
                        node = flatNode.node,
                        isRenaming = isRenaming,
                        baseName = baseName,
                        onAction = onAction
                    )
                }
            }

            if (!isDesktopPlatform) {
                Icon(
                    painter = painterResource(Res.drawable.more_vert),
                    tint = MaterialTheme.colorScheme.outline,
                    contentDescription = "open context menu",
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.iconMedium)
                        .clickable(
                            enabled = true,
                            onClick = { showMenu = true }
                        )
                )
            }

        }

        FileContextMenu(
            showMenu = showMenu,
            menuOffset = menuOffset,
            onDismissRequest = { showMenu = false },
            itemPath = flatNode.node.path,
            onAction = onAction,
            itemType = if (flatNode.node is FileNode.File) FileType.FILE else FileType.DIRECTORY,
        )
    }
}

@Composable
private fun RowScope.DirectoryContent(
    node: FileNode.Directory,
    isExpanded: Boolean,
    isRenaming: Boolean,
    baseName: String,
    onAction: (SidebarAction) -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)

    Icon(
        painter = painterResource(Res.drawable.arrow_right),
        contentDescription = null,
        modifier = Modifier
            .size(MaterialTheme.dimensions.iconSmall)
            .graphicsLayer { rotationZ = rotation },
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.width(6.dp))

    if (isRenaming) {
        RenameTextField(
            initialText = baseName,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.dimensions.listItemTextSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            onSubmit = { newName -> onAction(SidebarAction.RenameItem(newName, node.path)) },
            onCancel = { onAction(SidebarAction.SetRenameItem(null)) },
            modifier = Modifier.weight(1f)
        )
    } else {
        Text(
            text = baseName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.dimensions.listItemTextSize
            ),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RowScope.FileContent(
    node: FileNode.File,
    isRenaming: Boolean,
    baseName: String,
    onAction: (SidebarAction) -> Unit
) {
    Spacer(modifier = Modifier.width(20.dp))

    if (isRenaming) {
        RenameTextField(
            initialText = baseName,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.dimensions.listItemTextSize,
                color = MaterialTheme.colorScheme.onSurface
            ),
            onSubmit = { newName -> onAction(SidebarAction.RenameItem(newName, node.path)) },
            onCancel = { onAction(SidebarAction.SetRenameItem(null)) },
            modifier = Modifier.weight(1f)
        )
    } else {
        Text(
            text = baseName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = MaterialTheme.dimensions.listItemTextSize
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }

    val fileExtension = node.name.substringAfterLast('.', "")
    if (fileExtension.isNotEmpty() && fileExtension.lowercase() != "md") {
        Text(
            text = fileExtension.uppercase(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = MaterialTheme.dimensions.badgeTextSize
            ),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun RenameTextField(
    initialText: String,
    textStyle: TextStyle,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(0, initialText.length)))
    }

    var wasFocused by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { textFieldValue = it },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onSubmit(textFieldValue.text)
            submitted = true
        }),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp)
            )
            .clip(RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Enter -> {
                            onSubmit(textFieldValue.text)
                            submitted = true
                            true
                        }

                        Key.Escape -> {
                            onCancel()
                            true
                        }

                        else -> false
                    }
                } else false
            }
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    wasFocused = true
                } else {
                    if (wasFocused && !submitted) {
                        onSubmit(textFieldValue.text)
                    }
                }
            },
        singleLine = true,

        )
}

private fun Modifier.onRightClick(onEvent: (DpOffset) -> Unit): Modifier = composed {
    val density = LocalDensity.current
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    val position = event.changes.first().position
                    val offset = with(density) {
                        DpOffset(position.x.toDp(), (position.y - 10).toDp())
                    }
                    onEvent(offset)
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}