package dev.jotalac.feature.widget

import dev.jotalac.feature.editor.domain.EditorRepository
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import dev.jotalac.feature.notebooks_management.domain.Notebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform
import java.io.File

data class WidgetNote(
    val title: String,
    val path: String,
    val folder: String?,
    val preview: String,
    val lastModified: Long,
)

data class WidgetNotebook(
    val name: String,
    val notes: List<WidgetNote>,
)

/*
* access point for the android home screen widget (koin is not visible in the androidApp module)
* */
object WidgetNotesProvider {
    private const val MAX_NOTES = 30
    private const val PREVIEW_READ_CHARS = 600
    private const val PREVIEW_LENGTH = 140

    private val notebookRepository: NotebookRepository
        get() = KoinPlatform.getKoin().get()

    private val editorRepository: EditorRepository
        get() = KoinPlatform.getKoin().get()

    // emits null when there is no active notebook, re-emits whenever the active notebook changes
    fun activeNotebookFlow(): Flow<WidgetNotebook?> = notebookRepository.activeNotebookState
        .map { notebook -> notebook?.let(::loadNotebook) }
        .flowOn(Dispatchers.IO)

    private fun loadNotebook(notebook: Notebook): WidgetNotebook {
        val root = File(notebook.directoryPath)
        if (!root.isDirectory) return WidgetNotebook(notebook.name, emptyList())

        val notes = root.walkTopDown()
            // skip .git and other hidden folders
            .onEnter { it == root || !it.name.startsWith(".") }
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .sortedByDescending { it.lastModified() }
            .take(MAX_NOTES)
            .map { it.toWidgetNote(root) }
            .toList()

        return WidgetNotebook(notebook.name, notes)
    }

    // the editor opens the active note, so activating it is enough to open it on app start
    suspend fun openNote(notePath: String): Result<Unit> = notebookRepository.activateNote(notePath)

    // a new note in the root of the active notebook, made active so the editor opens it
    suspend fun createNote(): Result<Unit> {
        val notebook = notebookRepository.activeNotebookState.first()
            ?: return Result.failure(IllegalStateException("No active notebook"))
        val notePath = editorRepository.createNote(notebook.directoryPath).getOrElse { return Result.failure(it) }
        return notebookRepository.activateNote(notePath)
    }

    private fun File.toWidgetNote(root: File): WidgetNote {
        val folder = parentFile
            ?.takeIf { it != root }
            ?.relativeTo(root)
            ?.path

        return WidgetNote(
            title = nameWithoutExtension,
            path = absolutePath,
            folder = folder,
            preview = readPreview(),
            lastModified = lastModified(),
        )
    }

    private fun File.readPreview(): String {
        val head = runCatching {
            bufferedReader().use { reader ->
                val buffer = CharArray(PREVIEW_READ_CHARS)
                val read = reader.read(buffer)
                if (read > 0) String(buffer, 0, read) else ""
            }
        }.getOrDefault("")

        // remove the common markdown syntax from the widget preview
        return head.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("```") && !it.startsWith("$$") && !it.startsWith("![") }
            .map { it.trimStart('#', '>', '-', '*', '+', ' ').replace("**", "").replace("`", "").replace("~~", "") }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .take(PREVIEW_LENGTH)
    }
}
