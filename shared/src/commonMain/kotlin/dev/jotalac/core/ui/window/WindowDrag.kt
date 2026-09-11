package dev.jotalac.core.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Makes the window draggable - implemented only for desktop to make the app window movable (with
 * the custom top app bar).
 */
@Composable
internal expect fun Modifier.draggableWindow(): Modifier
