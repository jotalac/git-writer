package dev.jotalac.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow

class SnackbarManager {
    val messages = MutableSharedFlow<SnackbarText>(extraBufferCapacity = 1)

    fun showMessage(message: SnackbarText) {
        messages.tryEmit(message)
    }
}