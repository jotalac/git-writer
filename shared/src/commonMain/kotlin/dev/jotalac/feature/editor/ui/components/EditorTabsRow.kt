package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppHorizontalScrollbar
import dev.jotalac.core.ui.theme.dimensions
import dev.jotalac.feature.editor.domain.EditorTabItem
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.close
import git_writer.shared.generated.resources.new_tab
import git_writer.shared.generated.resources.plus
import git_writer.shared.generated.resources.x_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val TabShape = RoundedCornerShape(8.dp)

@Composable
fun EditorTabsRow(
    tabs: List<EditorTabItem>,
    activeTabId: Long,
    onItemClick: (EditorTabItem) -> Unit,
    onItemClose: (EditorTabItem) -> Unit,
    onNewTab: () -> Unit,
) {
    val rowState = rememberLazyListState()

    val isScrollable by remember {
        derivedStateOf { rowState.canScrollForward || rowState.canScrollBackward }
    }

    // keep the active tab visible when it changes
    LaunchedEffect(tabs, activeTabId) {
        val activeIndex = tabs.indexOfFirst { it.id == activeTabId }
        if (activeIndex != -1) {
            rowState.animateScrollToItem(activeIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp)
    ) {
        LazyRow(
            state = rowState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(tabs, key = { it.id }) { tab ->
                EditorTab(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    onClick = { onItemClick(tab) },
                    onClose = { onItemClose(tab) },
                )
            }

            item {
                IconButton(onClick = onNewTab) {
                    Icon(
                        painter = painterResource(Res.drawable.plus),
                        contentDescription = stringResource(Res.string.new_tab),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (isScrollable) {
            AppHorizontalScrollbar(
                rowState,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider(Modifier.height(2.dp).padding(top = 8.dp))
    }
}

@Composable
private fun EditorTab(
    tab: EditorTabItem,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(150.dp)
            .clip(TabShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                shape = TabShape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tab.filename ?: stringResource(Res.string.new_tab),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (tab.notePath != null) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.outline
            },
            style = MaterialTheme.typography.bodyMedium
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(MaterialTheme.dimensions.iconLarge)
        ) {
            Icon(
                painter = painterResource(Res.drawable.x_icon),
                contentDescription = stringResource(Res.string.close),
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(MaterialTheme.dimensions.iconMedium),
            )
        }
    }
}
