package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.ContextMenuItem
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.delete
import git_writer.shared.generated.resources.delete_item
import git_writer.shared.generated.resources.move_block_down
import git_writer.shared.generated.resources.move_block_up
import git_writer.shared.generated.resources.move_down
import git_writer.shared.generated.resources.move_up
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownBlockActionsBottomSheet(
    editorState: MarkdownEditorState,
    blockIndex: Int,
    onDismissRequest: () -> Unit,
    ) {
    val sheetState = rememberModalBottomSheetState()

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