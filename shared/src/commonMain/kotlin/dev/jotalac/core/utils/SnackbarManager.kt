package dev.jotalac.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow

class SnackbarManager {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    fun showMessage(message: String) {
        messages.tryEmit(message)
    }
}