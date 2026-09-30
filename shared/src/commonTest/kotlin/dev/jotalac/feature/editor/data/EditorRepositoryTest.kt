package dev.jotalac.feature.editor.data

import dev.jotalac.core.utils.deleteRecursively
import dev.jotalac.feature.editor.domain.EditorRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// integration tests for EditorRepositoryImpl - they run against the real filesystem
// inside a temporary directory that is deleted after every test
class EditorRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var editorRepository: EditorRepository
    private lateinit var testDir: Path

    private val defaultNoteName = "testNote"
    private val defaultNoteContent = "testContent"

    @BeforeTest
    fun setUp() {
        editorRepository = EditorRepositoryImpl(testDispatcher)

        // initialize directory where all the test files will be stored
        val baseDir = SystemTemporaryDirectory
        testDir = Path(baseDir, "git_writer_tests_${Random.nextInt()}")
        SystemFileSystem.createDirectories(testDir)
    }

    @AfterTest
    fun cleanup() {
        // delete the test files after each test
        if (SystemFileSystem.exists(testDir)) {
            testDir.deleteRecursively()
        }
    }

    private suspend fun createTestNote(
        noteName: String = defaultNoteName,
        noteContent: String = defaultNoteContent
    ): Result<String> = editorRepository.createNote(testDir.toString(), noteName, noteContent)

    private fun missingFilePath(): String = Path(testDir, "missing.md").toString()

    // ----------------------------------------------------------------- createNote

    @Test
    fun `createNote create note and writes content`() = runTest(testDispatcher) {
        val result = createTestNote()

        assertTrue(result.isSuccess)

        val createdFilePath = result.getOrThrow()
        val createdFile = Path(createdFilePath)

        assertTrue(SystemFileSystem.exists(createdFile))
        assertEquals("$defaultNoteName.md", createdFile.name)
        assertEquals(defaultNoteContent, PlatformFile(createdFile).readString())
    }

    @Test
    fun `createNote replaces invalid characters of the name`() = runTest(testDispatcher) {
        val result = createTestNote(noteName = "my:note?name")

        assertTrue(result.isSuccess)
        assertEquals("my_note_name.md", Path(result.getOrThrow()).name)
    }

    @Test
    fun `createNote changes the filename on duplicate name`() = runTest(testDispatcher) {
        val createFilePath = Path(createTestNote().getOrThrow())
        val createFilePath1 = Path(createTestNote().getOrThrow())
        val createFilePath2 = Path(createTestNote().getOrThrow())

        // every note has to end up in its own file
        assertEquals("$defaultNoteName.md", createFilePath.name)
        assertEquals("$defaultNoteName 1.md", createFilePath1.name)
        assertEquals("$defaultNoteName 2.md", createFilePath2.name)

        val paths = listOf(createFilePath, createFilePath1, createFilePath2)
        assertTrue(paths.all { SystemFileSystem.exists(it) })
    }

    // ------------------------------------------------------------ readNoteContent

    @Test
    fun `readNoteContent reads correct file content with new lines`() = runTest(testDispatcher) {
        val noteContent = "test note with new lines\n second line"
        val notePath = createTestNote(noteContent = noteContent).getOrThrow()

        assertEquals(noteContent, editorRepository.readNoteContent(notePath).getOrThrow())
    }

    @Test
    fun `readNoteContent fails for a missing file`() = runTest(testDispatcher) {
        assertTrue(editorRepository.readNoteContent(missingFilePath()).isFailure)
    }

    // ------------------------------------------------------------------ saveFile

    @Test
    fun `saveFile changes file content`() = runTest(testDispatcher) {
        val createFilePath = createTestNote().getOrThrow()
        val newFileContent = "new file content"

        assertTrue(editorRepository.saveFile(newFileContent, createFilePath).isSuccess)

        val readContent = editorRepository.readNoteContent(createFilePath).getOrThrow()
        assertEquals(newFileContent, readContent)
    }

    @Test
    fun `saveFile fails for a missing file`() = runTest(testDispatcher) {
        val result = editorRepository.saveFile("content", missingFilePath())

        assertTrue(result.isFailure)
        assertIs<IOException>(result.exceptionOrNull())
    }

    // ---------------------------------------------------------------- fileExists

    @Test
    fun `fileExists returns true for a file and false for missing paths and folders`() = runTest(testDispatcher) {
        val notePath = createTestNote().getOrThrow()
        val folderPath = Path(testDir, "someFolder")
        SystemFileSystem.createDirectories(folderPath)

        assertTrue(editorRepository.fileExists(notePath))
        assertFalse(editorRepository.fileExists(missingFilePath()))
        // a directory is not a file
        assertFalse(editorRepository.fileExists(folderPath.toString()))
    }

    // ----------------------------------------------------- loadMarkdownFileBlocks

    @Test
    fun `loadMarkdownFileBlocks splits the file into markdown blocks`() = runTest(testDispatcher) {
        val notePath = createTestNote(noteContent = "# h1\n# h2").getOrThrow()

        val blocks = editorRepository.loadMarkdownFileBlocks(notePath).getOrThrow()

        assertEquals(listOf("# h1", "# h2"), blocks)
    }

    @Test
    fun `loadMarkdownFileBlocks fails for a missing file`() = runTest(testDispatcher) {
        assertTrue(editorRepository.loadMarkdownFileBlocks(missingFilePath()).isFailure)
    }

    // ----------------------------------------------------------------- addFolder

    @Test
    fun `addFolder creates the folder`() = runTest(testDispatcher) {
        val folderPath = Path(testDir, "newFolder")

        assertTrue(editorRepository.addFolder("newFolder", testDir.toString()).isSuccess)
        assertTrue(SystemFileSystem.metadataOrNull(folderPath)?.isDirectory ?: false)
    }

    @Test
    fun `addFolder fails when the folder already exists`() = runTest(testDispatcher) {
        editorRepository.addFolder("newFolder", testDir.toString())

        val result = editorRepository.addFolder("newFolder", testDir.toString())

        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    // ----------------------------------------------------------------- moveItem

    @Test
    fun `moveItem moves the file into the destination directory`() = runTest(testDispatcher) {
        val notePath = Path(createTestNote().getOrThrow())
        val destinationDir = Path(testDir, "destination")
        SystemFileSystem.createDirectories(destinationDir)

        assertTrue(editorRepository.moveItem(notePath.toString(), destinationDir.toString()).isSuccess)

        assertTrue(SystemFileSystem.exists(Path(destinationDir, notePath.name)))
        assertFalse(SystemFileSystem.exists(notePath))
    }

    @Test
    fun `moveItem fails when the destination already contains the same name`() = runTest(testDispatcher) {
        val notePath = createTestNote().getOrThrow()
        val destinationDir = Path(testDir, "destination")
        SystemFileSystem.createDirectories(destinationDir)
        // a note with the same name already lives in the destination
        editorRepository.createNote(destinationDir.toString(), defaultNoteName, "other content")

        val result = editorRepository.moveItem(notePath, destinationDir.toString())

        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `moveItem fails when moving a folder into its own subfolder`() = runTest(testDispatcher) {
        val folderPath = Path(testDir, "folder")
        val subFolderPath = Path(folderPath, "sub")
        SystemFileSystem.createDirectories(subFolderPath)

        val result = editorRepository.moveItem(folderPath.toString(), subFolderPath.toString())

        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `moveItem into the same directory keeps the file in place`() = runTest(testDispatcher) {
        val notePath = Path(createTestNote().getOrThrow())

        val result = editorRepository.moveItem(notePath.toString(), testDir.toString())

        assertTrue(result.isSuccess)
        assertTrue(SystemFileSystem.exists(notePath))
    }

    // --------------------------------------------------------------- renameItem

    @Test
    fun `renameItem renames file`() = runTest(testDispatcher) {
        val createdFilePathString = createTestNote().getOrThrow()
        val createdFilePath = Path(createdFilePathString)
        val newName = "renamedNote.md"

        assertTrue(editorRepository.renameItem(createdFilePathString, newName).isSuccess)

        val newPath = Path(createdFilePath.parent!!, newName)
        assertTrue(SystemFileSystem.exists(newPath))
        assertFalse(SystemFileSystem.exists(createdFilePath))
    }

    @Test
    fun `renameItem to existing name should throw`() = runTest(testDispatcher) {
        val noteName = "note1"

        val firstNotePath = createTestNote().getOrThrow()
        assertTrue(createTestNote(noteName = noteName).isSuccess)

        val renameResult = editorRepository.renameItem(firstNotePath, "$noteName.md")

        assertTrue(renameResult.isFailure)
        assertIs<IllegalStateException>(renameResult.exceptionOrNull())
        // the file that was meant to be renamed is untouched
        assertTrue(SystemFileSystem.exists(Path(firstNotePath)))
    }

    // --------------------------------------------------------------- deleteItem

    @Test
    fun `deleteItem deletes file`() = runTest(testDispatcher) {
        val createFilePath = Path(createTestNote().getOrThrow())
        assertTrue(editorRepository.deleteItem(createFilePath.toString()).isSuccess)
        assertFalse(SystemFileSystem.exists(createFilePath))
    }

    @Test
    fun `deleteItem deletes a folder that is not empty`() = runTest(testDispatcher) {
        val folderPath = Path(testDir, "folder")
        SystemFileSystem.createDirectories(folderPath)
        editorRepository.createNote(folderPath.toString(), "nested", "nested content")

        assertTrue(editorRepository.deleteItem(folderPath.toString()).isSuccess)

        assertFalse(SystemFileSystem.exists(folderPath))
    }

    // ----------------------------------------------------------- savePastedImage

    @Test
    fun `savePastedImage creates the images folder and writes the bytes`() = runTest(testDispatcher) {
        val imageBytes = byteArrayOf(1, 2, 3, 4)

        assertTrue(editorRepository.savePastedImage(testDir.toString(), imageBytes, "image_one.png").isSuccess)

        val imagePath = Path(Path(testDir, "images"), "image_one.png")
        assertTrue(SystemFileSystem.exists(imagePath))
        assertContentEquals(imageBytes, PlatformFile(imagePath).readBytes())
    }

    @Test
    fun `savePastedImage writes into an already existing images folder`() = runTest(testDispatcher) {
        val firstBytes = byteArrayOf(1, 2)
        val secondBytes = byteArrayOf(3, 4)

        editorRepository.savePastedImage(testDir.toString(), firstBytes, "image_one.png")
        assertTrue(editorRepository.savePastedImage(testDir.toString(), secondBytes, "image_two.png").isSuccess)

        val imagesDir = Path(testDir, "images")
        assertTrue(SystemFileSystem.exists(Path(imagesDir, "image_one.png")))
        assertTrue(SystemFileSystem.exists(Path(imagesDir, "image_two.png")))
    }
}
