package dev.jotalac.feature.git_sync.encryption

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/*
* The encryption of an encrypted notebook's remote. Everything there is encrypted with one AES-256-GCM key,
* derived from the notebook's password - every device that knows the password derives the same key.
* */

/** The file in the remote's root that says the notebook is encrypted, and how to get its key. */
const val ENCRYPTION_CONFIG_FILE = "gitwriter-encryption.json"

// slow on purpose: every password guess costs as much (the OWASP recommendation for PBKDF2-HMAC-SHA256)
const val DEFAULT_KDF_ITERATIONS = 600_000

// what an encrypted file starts with - tells it apart from anything else, and leaves room for a new format
private val Magic = "GWENC1".encodeToByteArray()
private const val KEY_CHECK_TEXT = "git-writer key check"
private const val SALT_SIZE = 16

/**
 * What [ENCRYPTION_CONFIG_FILE] holds - nothing secret: how the key is derived from the password, and a text
 * encrypted with the key, to tell a wrong password apart before anything else is decrypted.
 */
@Serializable
data class EncryptionConfig(
    val version: Int = 1,
    val kdf: String = "PBKDF2-HMAC-SHA256",
    val iterations: Int,
    val salt: String,
    val keyCheck: String,
) {
    fun toJson(): String = ConfigJson.encodeToString(serializer(), this)

    companion object {
        fun fromJson(json: String): EncryptionConfig = ConfigJson.decodeFromString(serializer(), json)
    }
}

private val ConfigJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

/** A notebook's key. Kept on the device instead of the password, [encoded] as Base64. */
class NotebookKey(internal val bytes: ByteArray) {
    val encoded: String get() = Base64.encode(bytes)

    companion object {
        fun fromEncoded(encoded: String) = NotebookKey(Base64.decode(encoded))
    }
}

/** The password does not unlock the notebook. */
class WrongPasswordException : IllegalStateException("Wrong password for the encrypted notebook")

/** A file that is not encrypted with the notebook's key, or was changed - it cannot be decrypted. */
class DecryptionException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

object NotebookEncryption {

    private val provider get() = CryptographyProvider.Default

    /** A new key for [password], and the config that lets other devices derive it again. */
    suspend fun create(password: String, iterations: Int = DEFAULT_KDF_ITERATIONS): Pair<EncryptionConfig, NotebookKey> {
        val salt = CryptographyRandom.nextBytes(SALT_SIZE)
        val key = deriveKey(password, salt, iterations)
        val keyCheck = encrypt(key, KEY_CHECK_TEXT.encodeToByteArray(), ENCRYPTION_CONFIG_FILE)
        val config = EncryptionConfig(iterations = iterations, salt = Base64.encode(salt), keyCheck = Base64.encode(keyCheck))
        return config to key
    }

    /** The key [password] gives for [config]. Throws [WrongPasswordException] when it is not the right one. */
    suspend fun unlock(config: EncryptionConfig, password: String): NotebookKey {
        val key = deriveKey(password, Base64.decode(config.salt), config.iterations)
        val check = try {
            decrypt(key, Base64.decode(config.keyCheck), ENCRYPTION_CONFIG_FILE)
        } catch (_: DecryptionException) {
            throw WrongPasswordException()
        }
        if (check.decodeToString() != KEY_CHECK_TEXT) throw WrongPasswordException()
        return key
    }

    /**
     * [data] encrypted for the file at [path] (relative to the remote's root): the magic, a random nonce, the
     * ciphertext and its tag. The path is authenticated too, so a file moved to another path does not decrypt.
     */
    suspend fun encrypt(key: NotebookKey, data: ByteArray, path: String): ByteArray =
        Magic + cipher(key).encrypt(data, associatedData = path.encodeToByteArray())

    /** The data of a file [encrypt] wrote at [path]. Throws [DecryptionException] for anything else. */
    suspend fun decrypt(key: NotebookKey, encrypted: ByteArray, path: String): ByteArray {
        if (encrypted.size < Magic.size || !encrypted.copyOfRange(0, Magic.size).contentEquals(Magic)) {
            throw DecryptionException("$path is not an encrypted notebook file")
        }
        return try {
            cipher(key).decrypt(encrypted.copyOfRange(Magic.size, encrypted.size), associatedData = path.encodeToByteArray())
        } catch (e: Exception) {
            throw DecryptionException("$path cannot be decrypted - another key, or the file was changed", e)
        }
    }

    private suspend fun deriveKey(password: String, salt: ByteArray, iterations: Int): NotebookKey {
        val derivation = provider.get(PBKDF2).secretDerivation(
            digest = SHA256,
            iterations = iterations,
            outputSize = 256.bits,
            salt = salt,
        )
        return NotebookKey(derivation.deriveSecretToByteArray(password.encodeToByteArray()))
    }

    // a random 96-bit nonce per file, written in front of the ciphertext
    private suspend fun cipher(key: NotebookKey) =
        provider.get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key.bytes).cipher()
}
