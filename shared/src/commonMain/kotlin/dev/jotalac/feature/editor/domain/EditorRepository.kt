package dev.jotalac.feature.editor.domain

import io.github.vinceglb.filekit.PlatformFile

interface EditorRepository {
    suspend fun loadMarkdownFileBlocks(file: PlatformFile): Result<List<String>>
    suspend fun saveFile(fileContent: String, filePath: String): Result<Unit>
    suspend fun addNote(filename: String, filePath: String): Result<Unit>
    suspend fun addFolder(folderName: String, filePath: String): Result<Unit>
    suspend fun moveItem(sourcePath: String, destinationDirectoryPath: String): Result<Unit>
}