package dev.jotalac.feature.editor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.feature.editor.ui.components.MarkdownEditor

@Composable
fun EditorScreen() {
    // launched effects etc - load the markdown text to view
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

    EditorScreenContent(
        markdownContent = initialMarkdownContent
    )

}

@Composable
fun EditorScreenContent(
    markdownContent: String
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            MarkdownEditor(
                docTextContent = markdownContent,
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth()
            )
        }
    }
}