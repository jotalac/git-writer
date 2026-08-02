package dev.jotalac.core.utils

import androidx.compose.ui.Modifier

actual fun Modifier.onExternalImageDrop(
    onDragOverChange: (isDraggingOver: Boolean) -> Unit,
    onImageDropped: (imageBytesList: List<ByteArray>) -> Unit
): Modifier = this
