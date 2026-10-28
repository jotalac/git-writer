package dev.jotalac.feature.git_sync.encryption

import dev.jotalac.feature.git_sync.domain.MailboxRepository
import dev.jotalac.feature.git_sync.domain.MirrorRepository
import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import dev.jotalac.feature.git_sync.domain.PushRejectedException
import dev.jotalac.core.utils.suspendRunCatching
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The remote of a notebook that is being encrypted already has commits - readable ones would stay in its history. */
class RemoteNotEmptyException : IllegalStateException("The remote repository has to be empty to encrypt a notebook")

/** The remote is an encrypted notebook, opening it needs its password. */
class PasswordRequiredException : IllegalStateException("This notebook is encrypted - enter its password")

/** The remote is not one of an encrypted notebook. */
class NotEncryptedRemoteException : IllegalStateException("The remote repository is not an encrypted notebook")

/**
 * Sync of an encrypted notebook. The notebook stays a plain git repository - commits, merges and conflicts work as for
 * any other notebook - and its remote, the mailbox, only gets encrypted bundles: files with the commits of one upload
 * each. This class is the only one that sees both sides - it encrypts and decrypts between them.
 *
 * Per notebook, inside its `.git`:
 * - `gitwriter/mirror.git` ([MirrorRepository]) - what has been published, decrypted. It is the notebook's origin, so
 *   the plain sync ([GitSyncRepository]) runs against it unchanged.
 * - `gitwriter/mailbox` ([MailboxRepository]) - the copy of the mailbox: [ENCRYPTION_CONFIG_FILE] and
 *   `bundles/000001.bundle.enc`, ... - never anything readable.
 *
 * A sync downloads the bundles other devices published into the mirror, runs the plain sync against the mirror and
 * uploads what it pushed there as the next bundle. Bundles are numbered, each builds on the one before: when two
 * devices upload the same number, the second push is rejected - that device takes over the other's bundle and tries
 * again.
 */
class EncryptedGitSync(
    private val gitSync: GitSyncRepository,
    private val mirror: MirrorRepository,
    private val mailbox: MailboxRepository,
    private val kdfIterations: Int = DEFAULT_KDF_ITERATIONS,
) {

    /**
     * Encrypts the notebook at [notebookPath] from now on, with [password]: its history goes to the empty remote at
     * [remoteUrl] as the first bundle. Returns the key to keep for the notebook.
     */
    suspend fun enable(
        notebookPath: String,
        remoteUrl: String,
        tokenOrPassword: String,
        username: String?,
        password: String,
    ): Result<NotebookKey> = suspendRunCatching {
        val paths = NotebookPaths(notebookPath)
        mailbox.create(paths.mailbox, remoteUrl).getOrThrow()
        mailbox.update(paths.mailbox, tokenOrPassword, username).getOrThrow()
        if (!mailbox.isEmpty(paths.mailbox).getOrThrow()) throw RemoteNotEmptyException()

        val (config, key) = NotebookEncryption.create(password, kdfIterations)
        writeText(Path(paths.mailbox, ENCRYPTION_CONFIG_FILE), config.toJson())

        // the mirror starts with the notebook's history - a sync could not tell it is unpublished: the notebook still
        // has the old remote's branch, which already has all of it
        mirror.useMainBranch(notebookPath).getOrThrow()
        mirror.create(paths.mirror).getOrThrow()
        mirror.head(notebookPath).getOrThrow()?.let {
            mirror.importBundle(paths.mirror, mirror.createBundle(notebookPath, since = null).getOrThrow()).getOrThrow()
        }
        gitSync.updateRemoteUrl(notebookPath, paths.mirror).getOrThrow()
        paths.writeState(PublishState())

        // commits what is not committed yet and publishes everything, with the config, as the first bundle
        sync(notebookPath, key, tokenOrPassword, username, commitMessage = "encrypt notebook").getOrThrow()
        key
    }

    /**
     * Sets up the empty folder [notebookPath] for the encrypted notebook at [remoteUrl] and downloads its notes.
     * Fails with [WrongPasswordException] before anything is downloaded into the notebook.
     */
    suspend fun open(
        notebookPath: String,
        remoteUrl: String,
        tokenOrPassword: String,
        username: String?,
        password: String,

    ): Result<NotebookKey> = suspendRunCatching {
        val paths = NotebookPaths(notebookPath)
        gitSync.initRepository(notebookPath).getOrThrow()
        gitSync.cloneRepository(remoteUrl, tokenOrPassword, paths.mailbox, username).getOrThrow()

        val config = readConfig(paths) ?: throw NotEncryptedRemoteException()
        val key = NotebookEncryption.unlock(config, password)

        mirror.create(paths.mirror).getOrThrow()
        gitSync.updateRemoteUrl(notebookPath, paths.mirror).getOrThrow()
        paths.writeState(PublishState())

        when (val status = sync(notebookPath, key, tokenOrPassword, username).getOrThrow()) {
            is GitSyncStatus.Conflict -> error("A new notebook cannot have conflicts: $status")
            else -> key
        }
    }

    /**
     * Syncs the notebook: downloads the others' bundles, merges them and uploads what is new here - also a resolved
     * conflict, which is a merge commit the mirror does not have yet.
     */
    suspend fun sync(
        notebookPath: String,
        key: NotebookKey,
        tokenOrPassword: String,
        username: String?,
        commitMessage: String = "sync notes",
    ): Result<GitSyncStatus> = suspendRunCatching {
        val paths = NotebookPaths(notebookPath)
        // the mirror moves with the notebook (a renamed notebook, an iOS app container that changed with an update)
        gitSync.updateRemoteUrl(notebookPath, paths.mirror).getOrThrow()

        repeat(MAX_PUBLISH_ATTEMPTS) {
            download(paths, key, tokenOrPassword, username)

            val status = gitSync.syncNotes(notebookPath, tokenOrPassword, username, commitMessage).getOrThrow()
            // resolved later - the sync after that publishes the result
            if (status is GitSyncStatus.Conflict) return@suspendRunCatching status

            if (publish(paths, key, tokenOrPassword, username) != Publishing.Rejected) return@suspendRunCatching status
        }
        error("Other devices kept publishing at the same time - sync again")
    }

    // --- steps

    /** Brings the mailbox copy up to date and imports the bundles the mirror does not have yet, in their order. */
    private suspend fun download(paths: NotebookPaths, key: NotebookKey, tokenOrPassword: String, username: String?) {
        mailbox.update(paths.mailbox, tokenOrPassword, username).getOrThrow()

        var state = paths.readState()
        val bundleFiles = bundleFiles(paths)
        check(bundleFiles.size >= state.importedBundles) { "The remote lost bundles this device already has" }

        bundleFiles.drop(state.importedBundles).forEach { file ->
            val bundle = NotebookEncryption.decrypt(key, readBytes(Path(paths.mailbox, file)), file)
            val head = mirror.importBundle(paths.mirror, bundle).getOrThrow()
            state = PublishState(importedBundles = state.importedBundles + 1, publishedHead = head)
            paths.writeState(state)
        }
    }

    /**
     * Uploads the commits on the mirror's main since the last bundle as the next one. A remote that was never published
     * to gets the config even without a bundle - it makes the remote an encrypted notebook's.
     */
    private suspend fun publish(paths: NotebookPaths, key: NotebookKey, tokenOrPassword: String, username: String?): Publishing {
        val state = paths.readState()
        val head = mirror.head(paths.mirror).getOrThrow()
        val hasNewCommits = head != null && head != state.publishedHead
        val neverPublished = mailbox.isEmpty(paths.mailbox).getOrThrow()
        if (!hasNewCommits && !neverPublished) return Publishing.NothingNew

        val number = state.importedBundles + 1
        if (hasNewCommits) {
            val file = bundleFileName(number)
            val bundle = mirror.createBundle(paths.mirror, since = state.publishedHead).getOrThrow()
            writeBytes(Path(paths.mailbox, file), NotebookEncryption.encrypt(key, bundle, file))
        }

        val pushed = mailbox.publish(paths.mailbox, "bundle $number", tokenOrPassword, username)
        pushed.exceptionOrNull()?.let { if (it is PushRejectedException) return Publishing.Rejected else throw it }

        if (hasNewCommits) paths.writeState(PublishState(importedBundles = number, publishedHead = head))
        return if (hasNewCommits) Publishing.Published else Publishing.NothingNew
    }

    private fun readConfig(paths: NotebookPaths): EncryptionConfig? {
        val file = Path(paths.mailbox, ENCRYPTION_CONFIG_FILE)
        if (!SystemFileSystem.exists(file)) return null
        return EncryptionConfig.fromJson(readText(file))
    }

    // the bundles in their order - their numbers are padded, so sorting the names is enough
    private fun bundleFiles(paths: NotebookPaths): List<String> {
        val directory = Path(paths.mailbox, BUNDLES_DIRECTORY)
        if (!SystemFileSystem.exists(directory)) return emptyList()
        return SystemFileSystem.list(directory)
            .map { it.name }
            .filter { BundleFileRegex.matches(it) }
            .sorted()
            .map { "$BUNDLES_DIRECTORY/$it" }
    }

    private enum class Publishing { Published, NothingNew, Rejected }

    companion object {
        // bundles one device can lose a race for, before it gives up for this sync
        private const val MAX_PUBLISH_ATTEMPTS = 3

        /** Whether the cloned remote at [path] is an encrypted notebook. */
        fun isEncryptedRemote(path: String): Boolean = SystemFileSystem.exists(Path(path, ENCRYPTION_CONFIG_FILE))
    }
}

private const val BUNDLES_DIRECTORY = "bundles"
private val BundleFileRegex = Regex("""\d{6}\.bundle\.enc""")

/** The path of bundle [number] in the remote - also what its encryption is bound to. */
internal fun bundleFileName(number: Int) = "$BUNDLES_DIRECTORY/${number.toString().padStart(6, '0')}.bundle.enc"

/** How far the mirror is: the bundles it imported or published, and the commit the last one ended at. */
@Serializable
private data class PublishState(val importedBundles: Int = 0, val publishedHead: String? = null)

private val StateJson = Json { ignoreUnknownKeys = true }

/** Where an encrypted notebook keeps its mirror, mailbox copy and state - inside its `.git`. */
private class NotebookPaths(notebookPath: String) {
    private val root = Path(notebookPath, ".git", "gitwriter")
    val mirror = Path(root, "mirror.git").toString()
    val mailbox = Path(root, "mailbox").toString()
    private val stateFile = Path(root, "state.json")

    fun readState(): PublishState =
        if (SystemFileSystem.exists(stateFile)) StateJson.decodeFromString(PublishState.serializer(), readText(stateFile))
        else PublishState()

    fun writeState(state: PublishState) = writeText(stateFile, StateJson.encodeToString(PublishState.serializer(), state))
}

private fun readBytes(path: Path): ByteArray = SystemFileSystem.source(path).buffered().use { it.readByteArray() }

private fun readText(path: Path): String = SystemFileSystem.source(path).buffered().use { it.readString() }

private fun writeBytes(path: Path, bytes: ByteArray) {
    path.parent?.let { SystemFileSystem.createDirectories(it) }
    SystemFileSystem.sink(path).buffered().use { it.write(bytes) }
}

private fun writeText(path: Path, text: String) = writeBytes(path, text.encodeToByteArray())
