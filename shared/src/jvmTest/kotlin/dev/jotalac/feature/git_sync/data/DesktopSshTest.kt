package dev.jotalac.feature.git_sync.data

import kotlinx.coroutines.runBlocking
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.config.keys.PublicKeyEntry
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.junit.ssh.SshTestGitServer
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPair
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue

/*
* SSH remotes on desktop, against a local ssh git server: the user's home is a temporary folder, its `.ssh` holds the
* keys, and the ssh-agent is left out.
* */
class DesktopSshTest {

    private val temp = Files.createTempDirectory("ssh-test").toFile()
    private val home = File(temp, "home").apply { mkdirs() }
    private val sshDirectory = File(home, ".ssh")
    private val knownHosts = File(sshDirectory, "known_hosts")

    private val userKey = ed25519()
    private val repository = Git.init().setBare(true).setDirectory(File(temp, "remote.git")).call().repository
    private val factory = DesktopSshSessionFactory(homeDirectory = home, useAgent = false)
    private var server: SshTestGitServer? = null

    @AfterTest
    fun cleanUp() {
        server?.stop()
        SshSessionFactory.setInstance(null)
        factory.close()
        repository.close()
        temp.deleteRecursively()
    }

    private fun ed25519(): KeyPair = KeyUtils.generateKeyPair(KeyPairProvider.SSH_ED25519, 256)

    // the server lets in USER with [acceptedKey] - and with the password USER.uppercase() once passwords are enabled
    private fun startServer(acceptedKey: KeyPair = userKey, hostKey: KeyPair = ed25519()): String {
        val started = SshTestGitServer(USER, acceptedKey.public, repository, hostKey)
        server = started
        return "ssh://$USER@127.0.0.1:${started.start()}/remote.git"
    }

    private fun writeUserKey(passphrase: String? = null) {
        sshDirectory.mkdirs()
        val encryption = passphrase?.let {
            OpenSSHKeyEncryptionContext().apply {
                password = it
                cipherName = "AES"
                cipherType = "256"
                cipherMode = "CTR"
            }
        }
        File(sshDirectory, "id_ed25519").outputStream().use {
            OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(userKey, "test", encryption, it)
        }
    }

    // ls-remote, like when a notebook's remote is saved
    private fun connect(url: String, secret: String = ""): Result<Unit> = runBlocking {
        SshSessionFactory.setInstance(factory)
        JGitSyncRepositoryImpl().validateCredentials(url, secret, username = null)
    }

    @Test
    fun `the user's key signs in and a host seen for the first time is remembered`() {
        writeUserKey()
        val url = startServer()

        assertTrue(connect(url).isSuccess)
        assertTrue(knownHosts.readText().contains("127.0.0.1"), "the host is in known_hosts")
        // and once more, now checked against the remembered key
        assertTrue(connect(url).isSuccess)
    }

    @Test
    fun `a host whose key changed is refused`() {
        writeUserKey()
        val url = startServer()
        val port = url.substringAfter("127.0.0.1:").substringBefore('/')
        knownHosts.writeText("[127.0.0.1]:$port ${PublicKeyEntry.toString(ed25519().public)}\n")

        assertTrue(connect(url).isFailure)
    }

    @Test
    fun `a key with a passphrase opens with the notebook's password - and is skipped without it`() {
        writeUserKey(passphrase = "correct horse")
        val url = startServer()

        assertTrue(connect(url, secret = "").isFailure)
        assertTrue(connect(url, secret = "wrong").isFailure)
        // last: an unlocked key stays unlocked while the app runs, like in an ssh-agent
        assertTrue(connect(url, secret = "correct horse").isSuccess)
    }

    @Test
    fun `the notebook's password is never sent to the server as a login password`() {
        writeUserKey()
        // the user's ssh config allows passwords, and the server does not know the user's key - but would let in the
        // password the notebook has
        File(sshDirectory, "config").writeText("Host 127.0.0.1\n  PreferredAuthentications publickey,password\n")
        val url = startServer(acceptedKey = ed25519())
        server!!.enablePasswordAuthentication()
        val password = USER.uppercase()

        // what credentials that answer a password prompt would do: sign in with it
        val answeringPrompts = object : UsernamePasswordCredentialsProvider(USER, password) {
            override fun isInteractive() = true
        }
        SshSessionFactory.setInstance(factory)
        Git.lsRemoteRepository().setRemote(url).setCredentialsProvider(answeringPrompts).call()

        assertTrue(connect(url, secret = password).isFailure)
    }

    @Test
    fun `a user who never used ssh gets a private ssh folder with the host remembered`() {
        val url = startServer()

        // no key to sign in with - but the host was already checked
        assertTrue(connect(url).isFailure)
        assertTrue(knownHosts.readText().contains("127.0.0.1"))
        if ("posix" in sshDirectory.toPath().fileSystem.supportedFileAttributeViews()) {
            assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(sshDirectory.toPath()))
        }
    }

    private companion object {
        const val USER = "git"
    }
}
