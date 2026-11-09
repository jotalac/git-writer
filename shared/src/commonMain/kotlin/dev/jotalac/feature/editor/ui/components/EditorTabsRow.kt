package dev.jotalac.feature.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.components.AppHorizontalScrollbar
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.IconButton
import dev.jotalac.core.ui.design.Text
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.feature.editor.domain.EditorTabItem
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val TabShape = RoundedCornerShape(6.dp)

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
                IconButton(onClick = onNewTab, modifier = Modifier.size(AppTheme.dimensions.iconLarge)) {
                    Icon(
                        painter = painterResource(Res.drawable.plus),
                        contentDescription = stringResource(Res.string.new_tab),
                        tint = AppTheme.colors.accent,
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

    }
}

@Composable
private fun EditorTab(
    tab: EditorTabItem,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 250.dp)
            .border(
                width = 1.dp,
                color = if (isActive) {
                    AppTheme.colors.accent
                } else {
                    AppTheme.colors.divider.copy(alpha = 0.5f)
                },
                shape = TabShape
            )
            .clip(TabShape)
            .background(AppTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tab.filename ?: stringResource(Res.string.new_tab),
            modifier = Modifier.widthIn(max = 200.dp).padding(end = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (tab.notePath != null) {
                AppTheme.colors.text
            } else {
                AppTheme.colors.textMuted
            },
            style = AppTheme.typography.body
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(AppTheme.dimensions.iconLarge)
        ) {
            Icon(
                painter = painterResource(Res.drawable.x_icon),
                contentDescription = stringResource(Res.string.close),
                tint = AppTheme.colors.textMuted,
                modifier = Modifier.size(AppTheme.dimensions.iconMedium),
            )
        }
    }
}
