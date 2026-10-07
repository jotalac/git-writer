package dev.jotalac.testing.fakes

import dev.jotalac.feature.editor.domain.EditorRepository

data class SavedFile(val path: String, val content: String)

class FakeEditorRepository : EditorRepository {

    // what the next call returns
    var blocksResult: Result<List<String>> = Result.success(emptyList())
    var readResult: Result<String> = Result.success("")
    var saveResult: Result<Unit> = Result.success(Unit)
    var createNoteResult: Result<String> = Result.success("/notebook/untitled.md")
    var pastedImageResult: Result<Unit> = Result.success(Unit)

    /** Paths [fileExists] reports as present, so a test can drive the image branch per path. */
    val existingPaths: MutableSet<String> = mutableSetOf()

    // what production asked for
    val loadedPaths = mutableListOf<String>()
    val readPaths = mutableListOf<String>()
    val savedFiles = mutableListOf<SavedFile>()
    val createdNotes = mutableListOf<CreatedNote>()
    val addedFolders = mutableListOf<Pair<String, String>>()
    val movedItems = mutableListOf<Pair<String, String>>()
    val renamedItems = mutableListOf<Pair<String, String>>()
    val deletedPaths = mutableListOf<String>()
    val pastedImages = mutableListOf<PastedImage>()

    data class CreatedNote(val directoryPath: String, val baseName: String, val content: String)
    data class PastedImage(val notebookRootPath: String, val filename: String, val size: Int)

    override suspend fun loadMarkdownFileBlocks(filePath: String): Result<List<String>> {
        loadedPaths += filePath
        return blocksResult
    }

    override suspend fun fileExists(filePath: String): Boolean = filePath in existingPaths

    override suspend fun readNoteContent(filePath: String): Result<String> {
        readPaths += filePath
        return readResult
    }

    override suspend fun saveFile(fileContent: String, filePath: String): Result<Unit> {
        savedFiles += SavedFile(path = filePath, content = fileContent)
        return saveResult
    }

    override suspend fun createNote(
        directoryPath: String,
        baseName: String,
        noteContent: String,
    ): Result<String> {
        createdNotes += CreatedNote(directoryPath, baseName, noteContent)
        return createNoteResult
    }

    override suspend fun addFolder(folderName: String, filePath: String): Result<Unit> {
        addedFolders += filePath to folderName
        return Result.success(Unit)
    }

    override suspend fun moveItem(sourcePath: String, destinationDirectoryPath: String): Result<Unit> {
        movedItems += sourcePath to destinationDirectoryPath
        return Result.success(Unit)
    }

    override suspend fun renameItem(sourcePath: String, newName: String): Result<Unit> {
        renamedItems += sourcePath to newName
        return Result.success(Unit)
    }

    override suspend fun deleteItem(path: String): Result<Unit> {
        deletedPaths += path
        return Result.success(Unit)
    }

    override suspend fun savePastedImage(
        notebookRootPath: String,
        imageBytes: ByteArray,
        filename: String,
    ): Result<Unit> {
        pastedImages += PastedImage(notebookRootPath, filename, imageBytes.size)
        return pastedImageResult
    }
}
