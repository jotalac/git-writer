package dev.jotalac.feature.editor.domain

interface EditorRepository {
    suspend fun loadMarkdownFileBlocks(filePath: String): Result<List<String>>
    suspend fun fileExists(filePath: String): Boolean
    suspend fun readNoteContent(filePath: String): Result<String>
    suspend fun saveFile(fileContent: String, filePath: String): Result<Unit>
    suspend fun createNote(directoryPath: String, baseName: String = "untitled", noteContent: String = ""): Result<String>
    suspend fun addFolder(folderName: String, filePath: String): Result<Unit>
    suspend fun moveItem(sourcePath: String, destinationDirectoryPath: String): Result<Unit>
    suspend fun renameItem(sourcePath: String, newName: String): Result<Unit>
    suspend fun deleteItem(path: String): Result<Unit>
    suspend fun savePastedImage(notebookRootPath: String, imageBytes: ByteArray, filename: String): Result<Unit>
}
