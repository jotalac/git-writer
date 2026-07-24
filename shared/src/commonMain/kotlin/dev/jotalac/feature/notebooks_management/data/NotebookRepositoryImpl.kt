package dev.jotalac.feature.notebooks_management.data

import dev.jotalac.core.database.AppPreferencesManager
import dev.jotalac.core.utils.deleteRecursively
import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.io.files.Path
import kotlin.runCatching

class NotebookRepositoryImpl(
    private val notebookDao: NotebookDao,
    private val preferencesManager: AppPreferencesManager
) : NotebookRepository {
    override fun getAllNotebooks(): Flow<List<Notebook>> {
        return notebookDao.getNotebooksAsFlow().map { entityList ->
            entityList.map { entity -> entity.toNotebook() }
        }
    }

    private fun directoryExists(path: String): Boolean {
        val file = PlatformFile(path)
        return file.exists() && file.isDirectory()
    }

    override suspend fun createLocalNotebook(
        name: String, directoryPath: String,
        remoteUrl: String?, remoteUsername: String?, remotePassword: String?,
    ): Result<Notebook> {
        return runCatching {
            // first create the directory
            if (directoryExists(directoryPath)) {
                throw IllegalStateException("Directory already exists")
            }

            val baseDirectory = PlatformFile(directoryPath)
            baseDirectory.createDirectories()

            //create images directroy
            (baseDirectory / "images").createDirectories()
            // add starter file in the notebook (later there will be some factory for the files so reaplce this)
            val starterFile = PlatformFile(baseDirectory, "test.md")
            starterFile.writeString("# New notebook - $name \n- make sure to add your remote :)")

            //save notebook to database
            val notebook = Notebook(
                name = name.trim(),
                directoryPath = directoryPath,
                remoteUrl = remoteUrl?.trim(),
                remoteUsername = remoteUsername?.trim(),
                remotePassword = remotePassword,
            )
            val generatedId = notebookDao.upsertNotebook(notebook.toNotebookEntity())

            notebook.copy(id = generatedId)
        }
    }

    override suspend fun deleteNotebook(id: Long): Result<Unit> {
        return runCatching {
            val notebook = notebookDao.getNotebookById(id) ?: throw NullPointerException("Notebook with id $id not found")

            // delete the directory and all files inside
            val notebookDirectoryPath = Path(notebook.directoryPath)

            notebookDirectoryPath.deleteRecursively()

            notebookDao.deleteNotebook(notebook.id)

            if (preferencesManager.activeNotebookIdFlow.firstOrNull() == id) {
                preferencesManager.clearActiveNotebook()
            }
        }
    }

    override suspend fun activateNotebook(id: Long): Result<Unit> {
        return runCatching {
            val notebook = notebookDao.getNotebookById(id) ?: throw NullPointerException("Notebook not found")

            // check if the directory with the notebook content still exists
            if (!directoryExists(notebook.directoryPath)) {
                throw IllegalStateException("Notebook directory doesn't exist anymore (consider deleting the notebook)")
            }

            preferencesManager.setActiveNotebook(id)
        }
    }

    override fun getNotebookByIdAsFlow(id: Long): Flow<Notebook?> {
        return notebookDao.getNotebookByIdAsFlow(id).map { entity ->
            entity?.toNotebook()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val activeNotebook: Flow<Notebook?> = preferencesManager.activeNotebookIdFlow
        .flatMapLatest { activeId ->
            if (activeId == null) {
                flowOf(null)
            } else {
                getNotebookByIdAsFlow(activeId)
            }
        }

}