package dev.jotalac.feature.editor.data

import dev.jotalac.feature.editor.data.mapper.chunkMarkdownIntoBlocks
import dev.jotalac.feature.editor.domain.EditorRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
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

}