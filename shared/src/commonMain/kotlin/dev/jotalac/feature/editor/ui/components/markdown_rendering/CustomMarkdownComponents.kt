package dev.jotalac.feature.editor.ui.components.markdown_rendering

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.LatexAutoWrap
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownImage
import com.mikepenz.markdown.compose.elements.MarkdownListItems
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.compose.elements.listDepth
import org.intellij.markdown.ast.getTextInNode

// checkbox
fun customCheckboxComponent(onCheckedChange: (Int, Boolean) -> Unit): MarkdownComponent = { model ->
    val text = model.node.getTextInNode(model.content).toString()
    val checked = text.contains("[x]", ignoreCase = true)

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(model.node.startOffset, !checked) },
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

// unordered list
@Composable
fun CustomUnorderedListComponent(model: MarkdownComponentModel) {
    MarkdownListItems(
        content = model.content,
        node = model.node,
        depth = model.listDepth,
        bullet = { startNumber, index, child ->
            when (model.listDepth) {
                0 -> Text("• ")
                1 -> Text("◦ ")
                2 -> Text("▪ ")
                else -> Text("▫ ")
            }
        }
    )
}

// paragraph with latex block
@Composable
fun CustomParagraphComponent(model: MarkdownComponentModel) {
    if (!renderedLatexInParagraph(model)) {
        MarkdownParagraph(
            content = model.content,
            node = model.node
        )
    }
}

@Composable
private fun renderedLatexInParagraph(model: MarkdownComponentModel): Boolean {
    val mathChild = model.node.children.find { it.type.name in listOf("BLOCK_MATH", "INLINE_MATH") }
    return if (mathChild != null) {
        val mathText = mathChild.getTextInNode(model.content).toString()
            .removeSurrounding("$$")
            .removeSurrounding("$").trim()
        LatexAutoWrap(
            mathText,
            config = LatexConfig(
                theme = LatexTheme.material3()
            )
        )
        true
    } else {
        false
    }
}

// custom image component
@Composable
fun CustomImageComponent(model: MarkdownComponentModel) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
        MarkdownImage(content = model.content, node = model.node)
    }
}