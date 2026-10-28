package dev.jotalac.feature.git_sync.encryption

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotebookEncryptionTest {

    // a few iterations keep the tests fast - the derivation is the same with 600 000
    private val iterations = 1_000
    private val note = "# Diary\n\nnothing anyone else should read".encodeToByteArray()

    @Test
    fun `a file decrypts back to what was encrypted`() = runTest {
        val (_, key) = NotebookEncryption.create("correct horse", iterations)

        val encrypted = NotebookEncryption.encrypt(key, note, "bundles/000001.bundle.enc")

        assertContentEquals(note, NotebookEncryption.decrypt(key, encrypted, "bundles/000001.bundle.enc"))
    }

    @Test
    fun `the encrypted file shows nothing of the note`() = runTest {
        val (_, key) = NotebookEncryption.create("correct horse", iterations)

        val encrypted = NotebookEncryption.encrypt(key, note, "bundles/000001.bundle.enc")

        assertTrue(encrypted.decodeToString(throwOnInvalidSequence = false).startsWith("GWENC1"))
        assertFalse("Diary" in encrypted.decodeToString(throwOnInvalidSequence = false))
    }

    @Test
    fun `the same note encrypted twice looks different`() = runTest {
        // a random nonce per file - otherwise the remote would show which files have the same content
        val (_, key) = NotebookEncryption.create("correct horse", iterations)

        val first = NotebookEncryption.encrypt(key, note, "bundles/000001.bundle.enc")
        val second = NotebookEncryption.encrypt(key, note, "bundles/000001.bundle.enc")

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `another device gets the same key from the password`() = runTest {
        val (config, laptopKey) = NotebookEncryption.create("correct horse", iterations)
        val encrypted = NotebookEncryption.encrypt(laptopKey, note, "bundles/000001.bundle.enc")

        // the phone only has the config from the remote and the password
        val phoneKey = NotebookEncryption.unlock(EncryptionConfig.fromJson(config.toJson()), "correct horse")

        assertContentEquals(note, NotebookEncryption.decrypt(phoneKey, encrypted, "bundles/000001.bundle.enc"))
        assertEquals(laptopKey.encoded, phoneKey.encoded)
    }

    @Test
    fun `a wrong password is refused`() = runTest {
        val (config, _) = NotebookEncryption.create("correct horse", iterations)

        assertFailsWith<WrongPasswordException> { NotebookEncryption.unlock(config, "battery staple") }
    }

    @Test
    fun `a changed file does not decrypt`() = runTest {
        val (_, key) = NotebookEncryption.create("correct horse", iterations)
        val encrypted = NotebookEncryption.encrypt(key, note, "bundles/000001.bundle.enc")
        encrypted[encrypted.size - 1] = (encrypted.last() + 1).toByte()

        assertFailsWith<DecryptionException> { NotebookEncryption.decrypt(key, encrypted, "bundles/000001.bundle.enc") }
    }

    @Test
    fun `a file moved to another path does not decrypt`() = runTest {
        // otherwise an old bundle could be put in place of a newer one
        val (_, key) = NotebookEncryption.create("correct horse", iterations)
        val encrypted = NotebookEncryption.encrypt(key, note, "bundles/000001.bundle.enc")

        assertFailsWith<DecryptionException> { NotebookEncryption.decrypt(key, encrypted, "bundles/000002.bundle.enc") }
    }

    @Test
    fun `a file that is not encrypted is refused`() = runTest {
        val (_, key) = NotebookEncryption.create("correct horse", iterations)

        assertFailsWith<DecryptionException> { NotebookEncryption.decrypt(key, note, "notes/diary.md") }
    }

    @Test
    fun `the stored key works like the derived one`() = runTest {
        val (_, key) = NotebookEncryption.create("correct horse", iterations)
        val encrypted = NotebookEncryption.encrypt(key, note, "bundles/000001.bundle.enc")

        val stored = NotebookKey.fromEncoded(key.encoded)

        assertContentEquals(note, NotebookEncryption.decrypt(stored, encrypted, "bundles/000001.bundle.enc"))
    }
}
