package dev.jotalac

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCode
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

fun chunkMarkdownIntoBlocks(fullText: CharSequence): List<String> {
    // 1. Initialize the parser with GitHub Flavored Markdown rules
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour, cancellationToken = CancellationToken.NonCancellable)
    
    // 2. Generate the Tree (The Map)
    val parsedTree = parser.buildMarkdownTreeFromString(fullText)
    
    // 3. Slice the original text using the Tree's coordinates
    val blocks = mutableListOf<String>()
    
    for (node in parsedTree.children) {
        // We only want actual content blocks, not the empty white-space between them
        if (node.type.name != "WHITE_SPACE" && node.type.name != "EOL") {
            // Cut the specific chunk out of the original string
            val blockText = fullText.substring(node.startOffset, node.endOffset)
            blocks.add(blockText)
        }
    }
    
    return blocks
}

@Composable
fun MarkdownEditor(initialContent: String) {
    MaterialTheme {
        val blocks = remember {
            mutableStateListOf(*chunkMarkdownIntoBlocks(initialContent).toTypedArray())
        }

        var focusedIndex by remember { mutableStateOf<Int?>(null) }
        var cursorTarget by remember { mutableStateOf<TextRange?>(null) }
        val focusManager = LocalFocusManager.current
        val listState = rememberLazyListState()
        
        val surfaceFocusRequester = remember { FocusRequester() }

        LaunchedEffect(focusedIndex) {
            if (focusedIndex == null) {
                surfaceFocusRequester.requestFocus()
            }
        }

        fun addNewBlock() {
            if (focusedIndex != null) {
                // Unfocus the current block
                focusManager.clearFocus()
                focusedIndex = null
            } else {
                // If nothing is focused and they click the empty background, add a new block!
                blocks.add("")
                cursorTarget = TextRange(0)
                focusedIndex = blocks.lastIndex
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(blocks) { index, block ->
                    val bringIntoViewRequester = remember { BringIntoViewRequester() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .bringIntoViewRequester(bringIntoViewRequester)
                    ) {
                        if (focusedIndex == index) {
                            val focusRequester = remember { FocusRequester() }
                            var hasFocused by remember { mutableStateOf(false) }

                            var textFieldValue by remember(index) {
                                val initialSelection = cursorTarget ?: TextRange(block.length)
                                val safeSelection = TextRange(
                                    initialSelection.start.coerceIn(0, block.length),
                                    initialSelection.end.coerceIn(0, block.length)
                                )
                                mutableStateOf(TextFieldValue(text = block, selection = safeSelection))
                            }

                            LaunchedEffect(Unit) {
                                bringIntoViewRequester.bringIntoView()
                            }
                            
                            // Auto-scroll when typing pushes the cursor out of view
                            LaunchedEffect(textFieldValue.selection) {
                                bringIntoViewRequester.bringIntoView()
                            }

                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { 
                                    textFieldValue = it
                                    blocks[index] = it.text
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            hasFocused = true
                                        } else if (hasFocused) {
                                            // Element lost focus
                                            val currentFocused = focusedIndex
                                            
                                            if (textFieldValue.text.isBlank()) {
                                                if (index < blocks.size) {
                                                    blocks.removeAt(index)
                                                    if (currentFocused != null && currentFocused > index) {
                                                        focusedIndex = currentFocused - 1
                                                    }
                                                }
                                            } else {
                                                val currentText = blocks[index]
                                                val newChunks = chunkMarkdownIntoBlocks(currentText)
                                                if (newChunks.isEmpty()) {
                                                    blocks.removeAt(index)
                                                    if (currentFocused != null && currentFocused > index) {
                                                        focusedIndex = currentFocused - 1
                                                    }
                                                } else if (newChunks.size > 1) {
                                                    blocks.removeAt(index)
                                                    blocks.addAll(index, newChunks)
                                                    
                                                    if (currentFocused != null && currentFocused > index) {
                                                        focusedIndex = currentFocused + (newChunks.size - 1)
                                                    }
                                                }
                                            }

                                            if (focusedIndex == index) {
                                                focusedIndex = null
                                            }
                                        }
                                    }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            when (event.key) {
                                                Key.Escape -> {
                                                    focusManager.clearFocus()
                                                    focusedIndex = null
                                                    true
                                                }
                                                Key.Enter -> {
                                                    if (event.isCtrlPressed || event.isShiftPressed) {
                                                        blocks.add(index + 1, "")
                                                        cursorTarget = TextRange(0)
                                                        focusedIndex = index + 1
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                Key.DirectionUp -> {
                                                    val cursorStart = textFieldValue.selection.start
                                                    val firstNewline = textFieldValue.text.indexOf('\n')
                                                    val isFirstLine = if (firstNewline == -1) true else cursorStart <= firstNewline
                                                    
                                                    if (isFirstLine && index > 0) {
                                                        cursorTarget = TextRange(blocks[index - 1].length)
                                                        focusedIndex = index - 1
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                Key.DirectionDown -> {
                                                    val cursorStart = textFieldValue.selection.start
                                                    val lastNewline = textFieldValue.text.lastIndexOf('\n')
                                                    val isLastLine = if (lastNewline == -1) true else cursorStart > lastNewline
                                                    
                                                    if (isLastLine && index < blocks.size - 1) {
                                                        cursorTarget = TextRange(0)
                                                        focusedIndex = index + 1
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                Key.Backspace -> {
                                                    // delete the block if the content is empty
                                                    if (textFieldValue.text.isBlank()) {
                                                        blocks.removeAt(index)
                                                        focusedIndex = if (blocks.isNotEmpty() && focusedIndex != null) {
                                                            focusedIndex!! - 1
                                                        } else {
                                                            null
                                                        }
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                else -> false
                                            }
                                        } else {
                                            false
                                        }
                                    },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 24.dp)
                                    .clickable { 
                                        cursorTarget = null // Default to end of text
                                        focusedIndex = index 
                                    }
                            ) {
                                if (block.isBlank()) {
                                    Text(
                                        text = " ",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                } else {
                                    val isDarkTheme = isSystemInDarkTheme()
                                    val highlightsBuilder = remember(isDarkTheme) {
                                        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
                                    }

                                    Markdown(
                                        content = block,
                                        modifier = Modifier.fillMaxWidth(),
                                        components = markdownComponents(
                                            codeBlock = {
                                                MarkdownHighlightedCodeBlock(
                                                    content = it.content,
                                                    node = it.node,
                                                    highlightsBuilder = highlightsBuilder,
                                                    showHeader = true, // optional enable header with code language + copy button
                                                )
                                            },
                                            codeFence = {
                                                MarkdownHighlightedCodeFence(
                                                    content = it.content,
                                                    node = it.node,
                                                    highlightsBuilder = highlightsBuilder,
                                                    showHeader = true, // optional enable header with code language + copy button
                                                )
                                            },
                                            // doesn't work now :(, needed to render the math expressions at least somehow, later import the latex renderer
                                            custom = { type, model ->
                                                if (type == GFMElementTypes.BLOCK_MATH || type == GFMElementTypes.INLINE_MATH) {
                                                    MarkdownHighlightedCodeFence(
                                                        content = model.content,
                                                        node = model.node,
                                                        highlightsBuilder = highlightsBuilder,
                                                        showHeader = false,
                                                    )
                                                }
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .clickable {
                                blocks.add("")
                                cursorTarget = TextRange(0)
                                focusedIndex = blocks.lastIndex
                            }
                    ) {
                        Text(
                            text = "Tap to add new text...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun App() {
    val initialMarkdownContent = """
        # Hello Markdown
        
        This is a simple markdown example with:
        
        - Bullet points
        - **Bold text**
        - *Italic text*
        
        ```kotlin
        val myValue = 10
        fun thisIsFunction()
        ```
        
        [Check out this link](https://github.com/mikepenz/multiplatform-markdown-renderer)
    """.trimIndent()
    
    MarkdownEditor(initialContent = initialMarkdownContent)
}

@Composable
@Preview
fun EmptyEditorPreview() {
    MarkdownEditor(initialContent = "")
}