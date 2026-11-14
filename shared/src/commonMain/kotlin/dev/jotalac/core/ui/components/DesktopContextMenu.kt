package dev.jotalac.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

// space the menu keeps from the window's edges
private val WINDOW_MARGIN = 8.dp

/**
 * custom context menu that appears bellow the cursor, and shifts on screen end
 */
@Composable
fun DesktopContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset,
    content: @Composable () -> Unit,
) {
    if (!expanded) return

    val density = LocalDensity.current
    val positionProvider = remember(offset, density) {
        with(density) {
            PointerMenuPositionProvider(
                point = IntOffset(offset.x.roundToPx(), offset.y.roundToPx()),
                margin = WINDOW_MARGIN.roundToPx(),
            )
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        val focusManager = LocalFocusManager.current
        val inputModeManager = LocalInputModeManager.current
        val focusRequester = remember { FocusRequester() }
        // focused, so the arrow keys reach the menu before any of its items is
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Surface(
            shape = MenuDefaults.shape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f), shape = RoundedCornerShape(6.dp))
                    .padding(5.dp)
                    .width(200.dp)
                    .padding(vertical = 8.dp)
                    .width(IntrinsicSize.Max)
                    .verticalScroll(rememberScrollState())
                    // the arrow keys move between the items, like in Material's menu
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        val direction = when (event.key) {
                            Key.DirectionDown -> FocusDirection.Next
                            Key.DirectionUp -> FocusDirection.Previous
                            else -> return@onKeyEvent false
                        }
                        inputModeManager.requestInputMode(InputMode.Keyboard)
                        focusManager.moveFocus(direction)
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
            ) {
                DisableSelection {
                    content()
                }
            }
        }
    }
}

/**
 * Puts the menu's top-left corner at [point] (relative to the anchor's top-left) and moves it back into the
 * window, keeping [margin] from its edges, when it would stick out. A menu taller than the window starts at
 * the top - its items scroll.
 */
internal class PointerMenuPositionProvider(
    private val point: IntOffset,
    private val margin: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = fitInWindow(anchorBounds.left + point.x, popupContentSize.width, windowSize.width, margin),
        y = fitInWindow(anchorBounds.top + point.y, popupContentSize.height, windowSize.height, margin),
    )
}

private fun fitInWindow(start: Int, size: Int, space: Int, margin: Int): Int =
    start.coerceAtMost(space - margin - size).coerceAtLeast(margin)
