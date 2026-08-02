package dev.jotalac.core.utils

import androidx.compose.ui.Modifier

expect fun Modifier.onExternalImageDrop(
    onDragOverChange: (isDraggingOver: Boolean) -> Unit = {},
    onImageDropped: (imageBytesList: List<ByteArray>) -> Unit
): Modifier
