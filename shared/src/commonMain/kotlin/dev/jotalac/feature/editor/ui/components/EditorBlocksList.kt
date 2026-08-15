package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.utils.isDesktopPlatform
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
                        modifier = Modifier.combinedClickable(
                            onClick = { editorState.focusBlock(index, null) },
                            onLongClick = {
                                if (!isDesktopPlatform) {
                                    selectedBlockIndex = index
                                }
                            }
                        ),
                        onDeleteClick = { editorState.deleteBlock(index) }
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