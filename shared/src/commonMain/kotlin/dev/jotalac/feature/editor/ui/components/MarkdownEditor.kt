package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.editor.data.mapperw.chunkMarkdownIntoBlocks

@Composable
fun MarkdownEditor(
    docTextContent: String
) {
    val markdownBlocks = remember {
        mutableStateListOf(*chunkMarkdownIntoBlocks(docTextContent).toTypedArray())
    }

    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    var cursorTarget by remember { mutableStateOf<TextRange?>(null) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val surfaceFocusRequester = remember { FocusRequester() }

    // when focus changes request to enter new focus
    LaunchedEffect(focusedIndex) {
        if (focusedIndex == null) {
            surfaceFocusRequester.requestFocus()
        }
    }

    fun addNewBlock() {
        markdownBlocks.add("")
        cursorTarget = TextRange(0)
        focusedIndex = markdownBlocks.lastIndex
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            // add new block on enter press
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                    addNewBlock()
                    true
                } else false
            }
            .focusRequester(surfaceFocusRequester)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures {
                    addNewBlock()
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {

    }

}