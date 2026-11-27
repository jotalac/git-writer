package dev.jotalac.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.jotalac.core.utils.isDesktopPlatform

/**
 * A menu the way the platform shows it: at [offset] from its anchor on desktop, a sheet from the bottom on a phone.
 * [content] are its items - [ContextMenuItem]s.
 */
@Composable
fun AdaptiveMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable () -> Unit,
) {
    if (isDesktopPlatform) {
        DesktopContextMenu(expanded = expanded, onDismissRequest = onDismissRequest, offset = offset, content = content)
    } else {
        MenuSheet(expanded = expanded, onDismissRequest = onDismissRequest, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuSheet(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)

    if (expanded) {
        // the color of the sidebar and the menus, with the items inset from its edges like the sidebar's rows
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            CompositionLocalProvider(LocalMenuInSheet provides true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}
