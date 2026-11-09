package dev.jotalac.core.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

/**
 * The properties of the app's dialogs and sheets - sized by their content, over a [scrim]. A [fullScreen] one is laid
 * out over the whole window, under the system bars, and keeps out of them itself.
 */
internal expect fun appDialogProperties(dismissible: Boolean, fullScreen: Boolean, scrim: Color): DialogProperties
