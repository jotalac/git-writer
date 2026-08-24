package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.utils.isDesktopPlatform
import dev.jotalac.feature.editor.ui.EditorAction
import dev.jotalac.feature.editor.ui.MarkdownEditorState
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.add_new_block_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun MarkdownEditorBlocksList(
    blocks: List<String>,
    editorState: MarkdownEditorState,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    var selectedBlockIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 800.dp)
                .padding(16.dp)
        ) {
            blocks.forEachIndexed { index, block ->
                if (editorState.focusedIndex == index) {
                    ActiveEditorBlock(
                        editorState = editorState,
                        index = index
                    )
                } else {
                    RenderedEditorBlock(
                        text = block,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { editorState.focusBlock(index, null) },
                                onLongClick = {
                                    if (!isDesktopPlatform) {
                                        selectedBlockIndex = index
                                    }
                                }
                            ),
                        onDeleteClick = { editorState.deleteBlock(index) },
                        onTextChange = { newText ->
                            editorState.updateBlockText(index, newText)
                        }
                    )
                }
            }

            AddNewBlockButton(editorState)
            Spacer(modifier = Modifier.height(50.dp))

        }

        selectedBlockIndex?.let { index ->
            MarkdownBlockActionsBottomSheet(
                editorState = editorState,
                blockIndex = index,
                onDismissRequest = { selectedBlockIndex = null }
            )
        }
    }
}

@Composable
fun AddNewBlockButton(
    editorState: MarkdownEditorState,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable {
                editorState.addBlockAtEnd()
            }
    ) {
        Text(
            text = stringResource(Res.string.add_new_block_message),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}