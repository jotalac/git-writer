package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownListItems
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