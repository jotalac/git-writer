package dev.jotalac.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow

class SnackbarManager {
    val messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)

    fun showMessage(message: UiText) {
        messages.tryEmit(message)
    }
}