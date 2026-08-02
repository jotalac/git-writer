package dev.jotalac.feature.editor.data

import dev.jotalac.core.utils.deleteRecursively
import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks
import dev.jotalac.feature.editor.domain.EditorRepository
import io.github.vinceglb.filekit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString

class EditorRepositoryImpl : EditorRepository {
    override suspend fun loadMarkdownFileBlocks(file: PlatformFile): Result<List<String>> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val fileContent = file.readString()
                chunkMarkdownIntoBlocks(fileContent)
            }
        }
    }

    override suspend fun saveFile(fileContent: String, filePath: String): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val filePath = Path(filePath)

                if (!SystemFileSystem.exists(filePath)) throw IOException("File doesn't exist")

                SystemFileSystem.sink(filePath).buffered().use { buffer ->
                    buffer.writeString(fileContent)
                }
            }
        }
    }

    override suspend fun addNote(filename: String, filePath: String): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val newFile = Path(Path(filePath), filename)

                if (SystemFileSystem.exists(newFile)) {
                    throw IllegalStateException("File '$filename' already exists")
                }

                SystemFileSystem.sink(newFile).buffered().use { buffer ->
                    buffer.writeString("")
                }
            }
        }
    }

    override suspend fun addFolder(folderName: String, filePath: String): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val newFolder = PlatformFile(PlatformFile(filePath), folderName)

                if (newFolder.exists()) {
                    throw IllegalStateException("Folder '$folderName' already exists")
                }

                newFolder.createDirectories()
            }
        }
    }

    override suspend fun moveItem(sourcePath: String, destinationDirectoryPath: String): Result<Unit> {
        // if the source and destination are the same - dont do anything
        if (sourcePath.substringBeforeLast("/") == destinationDirectoryPath) return Result.success(Unit)
        // if the folder 
        if (destinationDirectoryPath == sourcePath || destinationDirectoryPath.startsWith("$sourcePath/")) {
            return Result.failure(IllegalStateException("Cannot move a folder into itself or its subfolder."))
        }

        return runCatching {
            withContext(Dispatchers.IO) {
                val source = Path(sourcePath)
                val destDir = Path(destinationDirectoryPath)
                val destFile = Path(destDir, source.name)

                if (SystemFileSystem.exists(destFile)) {
                    throw IllegalStateException("A file or folder named '${source.name}' already exists in this directory.")
                }

                SystemFileSystem.atomicMove(source, destFile)
            }
        }
    }

    override suspend fun renameItem(sourcePath: String, newName: String): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val source = Path(sourcePath)
                val destDir = source.parent ?: throw IllegalStateException("Invalid path")
                val destFile = Path(destDir, newName)

                if (SystemFileSystem.exists(destFile)) {
                    throw IllegalStateException("A file or folder named '$newName' already exists.")
                }

                SystemFileSystem.atomicMove(source, destFile)
            }
        }
    }

    override suspend fun deleteItem(path: String): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                Path(path).deleteRecursively()
            }
        }
    }

    override suspend fun savePastedImage(
        notebookRootPath: String,
        imageBytes: ByteArray,
        filename: String
    ): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val imageDir = PlatformFile(PlatformFile(notebookRootPath), "images")
                if (!imageDir.exists()) {
                    imageDir.createDirectories()
                }

                val imageFile = PlatformFile(imageDir, filename)
                imageFile.write(imageBytes)
            }
        }
    }
}