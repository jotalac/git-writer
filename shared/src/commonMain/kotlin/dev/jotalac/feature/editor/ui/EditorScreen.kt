package dev.jotalac.feature.editor.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

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
    Text(markdownContent)
}