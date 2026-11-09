package dev.jotalac.feature.sidebar.ui.components.file_tree

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.onContextMenuOpen
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.sidebar.domain.FileNode
import dev.jotalac.feature.sidebar.domain.FileType
import dev.jotalac.feature.sidebar.domain.FlatFileNode
import dev.jotalac.feature.sidebar.ui.SidebarAction
import dev.jotalac.feature.sidebar.ui.components.file_tree.context_menu.AdaptiveContextMenu
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.arrow_right
import git_writer.shared.generated.resources.expand_folders
import git_writer.shared.generated.resources.folder
import git_writer.shared.generated.resources.more_vert
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FileTreeRow(
    flatNode: FlatFileNode,
    isActive: Boolean = false,
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
                .onContextMenuOpen { offset ->
                    menuOffset = offset
                    showMenu = true
                }
                .then(dragModifier)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .clip(AppTheme.shapes.small)
                .then(if (showMenu || isActive) Modifier.background(AppTheme.colors.surfaceSunken) else Modifier)
                .clickable(
                    enabled = !isRenaming,
                    onClick = onClick
                )
                .padding(
                    start = (AppTheme.dimensions.listItemIndentPerLevel * flatNode.depth) + 4.dp,
                    top = AppTheme.dimensions.listItemPaddingVertical,
                    bottom = AppTheme.dimensions.listItemPaddingVertical,
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
                    tint = AppTheme.colors.textMuted,
                    contentDescription = "open context menu",
                    modifier = Modifier
                        .size(AppTheme.dimensions.iconMedium)
                        .clickable(
                            enabled = true,
                            onClick = { showMenu = true }
                        )
                )
            }

        }

        AdaptiveContextMenu(
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
            .size(AppTheme.dimensions.iconSmall)
            .graphicsLayer { rotationZ = rotation },
        tint = AppTheme.colors.textSecondary
    )
    Spacer(modifier = Modifier.width(6.dp))

    if (isRenaming) {
        RenameTextField(
            initialText = baseName,
            textStyle = AppTheme.typography.body.copy(
                fontSize = AppTheme.dimensions.listItemTextSize,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.text
            ),
            onSubmit = { newName -> onAction(SidebarAction.RenameItem(newName, node.path)) },
            onCancel = { onAction(SidebarAction.SetRenameItem(null)) },
            modifier = Modifier.weight(1f)
        )
    } else {
        // folder icon
        Icon(
            painter = painterResource(Res.drawable.folder),
            contentDescription = stringResource(Res.string.expand_folders),
            modifier = Modifier.size(AppTheme.dimensions.iconLarge).padding(end = 8.dp),
            tint = AppTheme.colors.textSecondary
        )

        // folder text
        Text(
            text = baseName,
            style = AppTheme.typography.body.copy(
                fontSize = AppTheme.dimensions.listItemTextSize
            ),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = AppTheme.colors.textSecondary,
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
            textStyle = AppTheme.typography.body.copy(
                fontSize = AppTheme.dimensions.listItemTextSize,
                color = AppTheme.colors.text
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
            style = AppTheme.typography.body.copy(
                fontSize = AppTheme.dimensions.listItemTextSize
            ),
            color = AppTheme.colors.text,
            modifier = Modifier.weight(1f)
        )
    }

    val fileExtension = node.name.substringAfterLast('.', "")
    if (fileExtension.isNotEmpty() && fileExtension.lowercase() != "md") {
        Text(
            text = fileExtension.uppercase(),
            style = AppTheme.typography.bodySmall.copy(
                fontSize = AppTheme.dimensions.badgeTextSize
            ),
            color = AppTheme.colors.textMuted
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
        cursorBrush = SolidColor(AppTheme.colors.text),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onSubmit(textFieldValue.text)
            submitted = true
        }),
        modifier = modifier
            .background(AppTheme.colors.surfaceSunken)
            .border(
                width = 1.dp,
                color = AppTheme.colors.accent,
                shape = AppTheme.shapes.small
            )
            .clip(AppTheme.shapes.small)
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

