package dev.jotalac.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/*
* Opens databases the way the installed app left them, with the app's real SQLite driver - the schemas of the
* older versions are the ones Room exported (shared/schemas).
* */
class DatabaseMigrationTest {

    private val folder = Files.createTempDirectory("database-migration-test").toFile()
    private val databaseFile = File(folder, "app_db.db")

    @AfterTest
    fun cleanUp() {
        folder.deleteRecursively()
    }

    @Test
    fun `notebooks saved before encryption existed open as unencrypted ones`() = runTest {
        createDatabase(version = 1)
        BundledSQLiteDriver().open(databaseFile.path).use { connection ->
            connection.execSQL(
                "INSERT INTO notebooks (id, name, directoryPath, remoteUrl, remoteUsername, remotePassword) " +
                    "VALUES (7, 'Diary', '/notes/Diary', 'https://example.com/diary.git', 'me', 'token')"
            )
        }

        val database = Room.databaseBuilder<AppDatabase>(name = databaseFile.path)
            .setDriver(BundledSQLiteDriver())
            .build()
        val notebook = try {
            database.getNotebooksDao().getNotebooksAsFlow().first().single()
        } finally {
            database.close()
        }

        assertEquals("Diary", notebook.name)
        assertEquals("token", notebook.remotePassword)
        assertFalse(notebook.isEncrypted)
        assertNull(notebook.encryptionKey)
    }

    /** An empty database as [version] of the app created it, from the schema Room exported for it. */
    private fun createDatabase(version: Int) {
        val schema = Json.parseToJsonElement(File("schemas/dev.jotalac.core.database.AppDatabase/$version.json").readText())
            .jsonObject.getValue("database").jsonObject
        BundledSQLiteDriver().open(databaseFile.path).use { connection ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val table = entity.jsonObject.getValue("tableName").jsonPrimitive.content
                connection.execSQL(entity.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
            schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
            connection.execSQL("PRAGMA user_version = $version")
        }
    }
}
