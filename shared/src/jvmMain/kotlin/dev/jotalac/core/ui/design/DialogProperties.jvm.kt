package dev.jotalac.core.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

internal actual fun appDialogProperties(dismissible: Boolean, fullScreen: Boolean, scrim: Color): DialogProperties =
    DialogProperties(
        dismissOnBackPress = dismissible,
        dismissOnClickOutside = dismissible,
        usePlatformDefaultWidth = false,
        usePlatformInsets = !fullScreen,
        // a sheet keeps out of the keyboard itself
        useSoftwareKeyboardInset = !fullScreen,
        scrimColor = scrim,
    )
