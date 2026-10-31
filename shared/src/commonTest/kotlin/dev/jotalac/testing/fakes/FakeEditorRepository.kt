package dev.jotalac.testing.fakes

import dev.jotalac.feature.editor.domain.EditorRepository

data class SavedFile(val path: String, val content: String)

class FakeEditorRepository : EditorRepository {

    // what the next call returns
    var readResult: Result<String> = Result.success("")
    var saveResult: Result<Unit> = Result.success(Unit)
    var createNoteResult: Result<String> = Result.success("/notebook/untitled.md")
    var pastedImageResult: Result<Unit> = Result.success(Unit)
    var addFolderResult: Result<Unit> = Result.success(Unit)
    var moveResult: Result<Unit> = Result.success(Unit)
    var renameResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    /** Paths [fileExists] reports as present, so a test can drive the image branch per path. */
    val existingPaths: MutableSet<String> = mutableSetOf()

    // what production asked for
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
        return addFolderResult
    }

    override suspend fun moveItem(sourcePath: String, destinationDirectoryPath: String): Result<Unit> {
        movedItems += sourcePath to destinationDirectoryPath
        return moveResult
    }

    override suspend fun renameItem(sourcePath: String, newName: String): Result<Unit> {
        renamedItems += sourcePath to newName
        return renameResult
    }

    override suspend fun deleteItem(path: String): Result<Unit> {
        deletedPaths += path
        return deleteResult
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
