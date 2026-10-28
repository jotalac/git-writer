package dev.jotalac.feature.git_sync.data

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.random.Random

/** A folder in the temp directory for the repositories of one test - [delete] removes it with everything in it. */
class TestFolder(name: String) {
    val root = Path(SystemTemporaryDirectory, "$name-${Random.nextLong().toULong()}").also { SystemFileSystem.createDirectories(it) }

    fun path(name: String): String = Path(root, name).toString()

    fun write(directory: String, file: String, text: String) {
        val path = Path(directory, file)
        path.parent?.let { SystemFileSystem.createDirectories(it) }
        SystemFileSystem.sink(path).buffered().use { it.writeString(text) }
    }

    fun read(directory: String, file: String): String =
        SystemFileSystem.source(Path(directory, file)).buffered().use { it.readString() }

    fun delete() = deleteRecursively(root)

    private fun deleteRecursively(path: Path) {
        if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) {
            SystemFileSystem.list(path).forEach(::deleteRecursively)
        }
        SystemFileSystem.delete(path, mustExist = false)
    }
}
