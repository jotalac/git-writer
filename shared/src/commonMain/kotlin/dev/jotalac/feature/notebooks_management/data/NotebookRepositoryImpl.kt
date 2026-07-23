package dev.jotalac.feature.notebooks_management.data

import dev.jotalac.feature.notebooks_management.domain.Notebook
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotebookRepositoryImpl(
    private val notebookDao: NotebookDao
) : NotebookRepository {
    override fun getAllNotebooks(): Flow<List<Notebook>> {
        return notebookDao.getNotebooksAsFlow().map { entityList ->
            entityList.map { entity -> entity.toNotebook() }
        }
    }

    override suspend fun createLocalNotebook(
        name: String, directoryPath: String,
        remoteUrl: String?, remoteUsername: String?, remotePassword: String?,
    ): Result<Notebook> {
        return runCatching {
            // first create the directory
            val file = PlatformFile(directoryPath)

            if (file.exists()) {
                throw IllegalStateException("Directory already exists")
            }

            file.createDirectories()
            // add starter file in the notebook
            val starterFile = PlatformFile(file, "test.md")
            starterFile.writeString("# New notebook - $name \n- make sure to add your remote :)")

            //save notebook to database
            val notebook = Notebook(
                name = name,
                directoryPath = directoryPath,
                remoteUrl = remoteUrl,
                remoteUsername = remoteUsername,
                remotePassword = remotePassword,
            )
            val generatedId = notebookDao.upsertNotebook(notebook.toNotebookEntity())

            notebook.copy(id = generatedId.toInt())
        }
    }

    override suspend fun deleteNotebook(id: Int): Result<Unit> {
        return runCatching {
            notebookDao.deleteNotebook(id)
        }
    }

}