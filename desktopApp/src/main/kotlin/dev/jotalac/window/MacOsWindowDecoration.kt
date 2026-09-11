package dev.jotalac.window

import androidx.compose.ui.awt.ComposeWindow

// make macOS draw the traffic lights
internal fun ComposeWindow.applyMacOsUnifiedTitleBar() {
    with(rootPane) {
        putClientProperty("apple.awt.transparentTitleBar", true)
        putClientProperty("apple.awt.fullWindowContent", true)
        putClientProperty("apple.awt.windowTitleVisible", false)
    }
}
