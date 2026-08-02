package dev.jotalac.core.utils

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.datatransfer.DataFlavor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onExternalImageDrop(
    onDragOverChange: (isDraggingOver: Boolean) -> Unit,
    onImageDropped: (imageBytesList: List<ByteArray>) -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val currentOnDragOverChange by rememberUpdatedState(onDragOverChange)
    val currentOnImageDropped by rememberUpdatedState(onImageDropped)

    val shouldStart = remember {
        { event: DragAndDropEvent ->
            val transferable = event.awtTransferable
            transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    transferable.isDataFlavorSupported(DataFlavor.imageFlavor)
        }
    }

    val target = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                currentOnDragOverChange(true)
            }

            override fun onEntered(event: DragAndDropEvent) {
                currentOnDragOverChange(true)
            }

            override fun onExited(event: DragAndDropEvent) {
                currentOnDragOverChange(false)
            }

            override fun onEnded(event: DragAndDropEvent) {
                currentOnDragOverChange(false)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentOnDragOverChange(false)
                val transferable = event.awtTransferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                    val imageFiles = files?.filterIsInstance<File>()?.filter { isImageFile(it.name) } ?: emptyList()
                    if (imageFiles.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            val bytesList = imageFiles.filter { it.exists() }.map { it.readBytes() }
                            if (bytesList.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    currentOnImageDropped(bytesList)
                                }
                            }
                        }
                        return true
                    }
                }
                if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    val rawImage = transferable.getTransferData(DataFlavor.imageFlavor) as? java.awt.Image
                    if (rawImage != null) {
                        scope.launch(Dispatchers.Default) {
                            val bytes = rawImage.toPngByteArray()
                            if (bytes != null) {
                                withContext(Dispatchers.Main) {
                                    currentOnImageDropped(listOf(bytes))
                                }
                            }
                        }
                        return true
                    }
                }
                return false
            }
        }
    }

    this.dragAndDropTarget(
        shouldStartDragAndDrop = shouldStart,
        target = target
    )
}
