package dev.jotalac.core.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

// the scrim is the dialog window's own dim on Android
internal actual fun appDialogProperties(dismissible: Boolean, fullScreen: Boolean, scrim: Color): DialogProperties =
    DialogProperties(
        dismissOnBackPress = dismissible,
        dismissOnClickOutside = dismissible,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = !fullScreen,
    )
