package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.ContextMenuItem
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownBlockActionsBottomSheet(
    editorState: MarkdownEditorState,
    blockIndex: Int,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 24.dp)
        ) {
            //delete block
            ContextMenuItem(
                text = stringResource(Res.string.delete_item),
                iconPainter = Res.drawable.delete,
                onClick = { editorState.deleteBlock(blockIndex) },
                onDismissRequest = onDismissRequest,
                itemColor = MaterialTheme.colorScheme.error
            )

            HorizontalDivider()

            // move blocks up and down
            ContextMenuItem(
                text = stringResource(Res.string.move_block_up),
                iconPainter = Res.drawable.move_up,
                onClick = { editorState.swapBlockUp(blockIndex) },
                onDismissRequest = onDismissRequest
            )

            ContextMenuItem(
                text = stringResource(Res.string.move_block_down),
                iconPainter = Res.drawable.move_down,
                onClick = { editorState.swapBlockDown(blockIndex) },
                onDismissRequest = onDismissRequest
            )
        }
    }
}

// might put it back later

//@Composable
//fun MarkdownBlockActionsContextMenu(
//    showMenu: Boolean,
//    editorState: MarkdownEditorState,
//    blockIndex: Int,
//    onDismissRequest: () -> Unit,
//    menuOffset: DpOffset,
//) {
//
//
//    DesktopContextMenu(
//        expanded = showMenu,
//        onDismissRequest = onDismissRequest,
//        offset = menuOffset
//    ) {
//        ContextMenuItem(
//            text = stringResource(Res.string.delete_item),
//            iconPainter = Res.drawable.delete,
//            onClick = { editorState.deleteBlock(blockIndex) },
//            onDismissRequest = onDismissRequest,
//            itemColor = MaterialTheme.colorScheme.error
//        )
//
//        HorizontalDivider()
//
//        ContextMenuItem(
//            text = stringResource(Res.string.move_block_up),
//            iconPainter = Res.drawable.move_up,
//            onClick = { editorState.swapBlockUp(blockIndex) },
//            onDismissRequest = onDismissRequest
//        )
//
//        ContextMenuItem(
//            text = stringResource(Res.string.move_block_down),
//            iconPainter = Res.drawable.move_down,
//            onClick = { editorState.swapBlockDown(blockIndex) },
//            onDismissRequest = onDismissRequest
//        )
//    }
//}