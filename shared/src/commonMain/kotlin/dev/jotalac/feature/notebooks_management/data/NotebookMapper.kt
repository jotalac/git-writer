package dev.jotalac.feature.notebooks_management.data

import dev.jotalac.feature.notebooks_management.domain.Notebook

fun NotebookEntity.toNotebook(): Notebook {
    return Notebook(
        id = this.id,
        name = this.name,
        directoryPath = this.directoryPath,
        remoteUrl = this.remoteUrl,
        remoteUsername = this.remoteUsername,
        remotePassword = this.remotePassword,
        isEncrypted = this.isEncrypted,
        encryptionKey = this.encryptionKey,
    )
}

fun Notebook.toNotebookEntity(): NotebookEntity {
    return NotebookEntity(
        id = this.id,
        name = this.name,
        directoryPath = this.directoryPath,
        remoteUrl = this.remoteUrl,
        remoteUsername = this.remoteUsername,
        remotePassword = this.remotePassword,
        isEncrypted = this.isEncrypted,
        encryptionKey = this.encryptionKey,
    )
}