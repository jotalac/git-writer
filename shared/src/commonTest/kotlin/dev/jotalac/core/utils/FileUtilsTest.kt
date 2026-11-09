package dev.jotalac.core.utils

import dev.jotalac.feature.sidebar.domain.FileNode
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.writeString
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileUtilsTest {

    private lateinit var testDir: Path

    @BeforeTest
    fun createTestDir() {
        testDir = Path(SystemTemporaryDirectory, "git_writer_file_utils_test_${Random.nextInt()}")
        SystemFileSystem.createDirectories(testDir)
    }

    @AfterTest
    fun deleteTestDir() {
        testDir.deleteRecursively()
    }

    private fun file(relativePath: String): Path {
        val path = Path(testDir, relativePath)
        path.parent?.let { SystemFileSystem.createDirectories(it) }
        SystemFileSystem.sink(path).buffered().use { it.writeString("content") }
        return path
    }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    // --- toSafeFileName

    @Test
    fun `unsafe filename signs gets replace with underscores`() {
        assertEquals("test_file", "test/file".toSafeFileName())
    }

    @Test
    fun `every character windows or macos forbid in a file name is replaced`() {
        assertEquals("a_b_c_d_e_f_g_h_i_j", "a\\b/c:d*e?f\"g<h>i|j".toSafeFileName())
    }

    @Test
    fun `surrounding spaces and trailing dots are removed`() {
        // windows cannot create a file or folder that ends with a dot
        assertEquals("notes", "  notes.  ".toSafeFileName())
    }

    // --- deleteRecursively

    @Test
    fun `a folder is deleted with everything inside it`() {
        file("notebook/images/photo.png")
        file("notebook/.git/HEAD")
        file("notebook/note.md")

        Path(testDir, "notebook").deleteRecursively()

        assertFalse(SystemFileSystem.exists(Path(testDir, "notebook")))
    }

    @Test
    fun `deleting something that does not exist is not an error`() {
        Path(testDir, "missing").deleteRecursively()
    }

    // --- the file tree

    @Test
    fun `the file tree has notes and images only and skips hidden files`() {
        file("note.md")
        file("PHOTO.PNG")
        file("todo.txt")
        file(".hidden.md")
        file(".git/config")

        val names = Path(testDir, "").buildFileTree().children.map { it.name }

        assertEquals(setOf("note.md", "PHOTO.PNG"), names.toSet())
    }

    @Test
    fun `folders come first and names are sorted alphabetically ignoring case`() {
        // sorted by character code, every capitalised name would come before all lowercase ones
        file("Zoo/inside.md")
        file("archive/inside.md")
        file("banana.md")
        file("Cherry.md")
        file("apple.md")

        val names = testDir.buildFileTree().children.map { it.name }

        assertEquals(listOf("archive", "Zoo", "apple.md", "banana.md", "Cherry.md"), names)
    }

    @Test
    fun `folders contain their own tree`() {
        file("projects/2026/plan.md")

        val projects = testDir.buildFileTree().children.single() as FileNode.Directory
        val year = projects.children.single() as FileNode.Directory

        assertEquals(listOf("plan.md"), year.children.map { it.name })
    }

    // --- images

    @Test
    fun `image files are recognised whatever the case of the extension`() {
        assertTrue(isImageFile("photo.JPEG"))
        assertTrue(isImageFile("diagram.svg"))
        assertFalse(isImageFile("photo.png.md"))
        assertFalse(isImageFile("notes.md"))
    }

    @Test
    fun `a pasted image gets the extension of its real format`() {
        assertEquals(".jpg", bytes(0xFF, 0xD8, 0xFF, 0xE0).detectImageExtension())
        assertEquals(".png", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A).detectImageExtension())
        assertEquals(".gif", "GIF89a".encodeToByteArray().detectImageExtension())
        assertEquals(".webp", "RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".encodeToByteArray().detectImageExtension())
        assertEquals(".svg", "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\">".encodeToByteArray().detectImageExtension())
    }

    @Test
    fun `a riff file that is not webp is not saved as webp`() {
        // RIFF is also the container of .wav sound files
        assertEquals(".png", "RIFF\u0000\u0000\u0000\u0000WAVEfmt ".encodeToByteArray().detectImageExtension())
    }
}
