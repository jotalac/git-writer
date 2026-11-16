package dev.jotalac.feature.git_sync.data

import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.sshd.DefaultProxyDataFactory
import org.eclipse.jgit.transport.sshd.JGitKeyCache
import org.eclipse.jgit.transport.sshd.KeyPasswordProvider
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase.Configuration.StrictHostKeyChecking
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.PublicKey

// register a factory for ssh, that jgit uses
fun jgitUseDesktopSsh() {
    SshSessionFactory.setInstance(DesktopSshSessionFactory())
}

// ssh session for application - arguments are only changed in tests for proper testability
internal class DesktopSshSessionFactory(
    homeDirectory: File? = null,
    private val useAgent: Boolean = true,
) : SshdSessionFactory(JGitKeyCache(), DefaultProxyDataFactory()) {

    init {
        if (homeDirectory != null) {
            this.homeDirectory = homeDirectory
            this.sshDirectory = File(homeDirectory, SSH_DIRECTORY)
        }
    }

    // keys only - the user's ssh config can still allow more for a host, but a password is never sent (GitCredentials)
    override fun getDefaultPreferredAuthentications(): String = "publickey"

    override fun getConnectorFactory(): ConnectorFactory? = if (useAgent) super.getConnectorFactory() else null

    override fun createKeyPasswordProvider(provider: CredentialsProvider?): KeyPasswordProvider =
        NotebookKeyPassphrase((provider as? GitCredentials)?.secret)

    override fun createServerKeyDatabase(homeDir: File, sshDir: File): ServerKeyDatabase {
        createKnownHostsFile(sshDir)
        return AcceptNewHosts(super.createServerKeyDatabase(homeDir, sshDir))
    }
}

private const val SSH_DIRECTORY = ".ssh"
private const val KNOWN_HOSTS_FILE = "known_hosts"

// passphrase of an ssh key, ensures that the same wrong key is only tried once
private class NotebookKeyPassphrase(private val passphrase: String?) : KeyPasswordProvider {

    override fun getPassphrase(uri: URIish?, attempt: Int): CharArray? =
        passphrase?.takeIf { it.isNotEmpty() }?.toCharArray()

    override fun setAttempts(numberOfPasswordPrompts: Int) = Unit

    override fun getAttempts(): Int = 1

    override fun keyLoaded(uri: URIish?, attempt: Int, error: Exception?): Boolean = false
}


// accept unknown host and add it to know_hosts file
internal class AcceptNewHosts(private val delegate: ServerKeyDatabase) : ServerKeyDatabase by delegate {
    override fun accept(
        connectAddress: String,
        remoteAddress: InetSocketAddress,
        serverKey: PublicKey,
        config: ServerKeyDatabase.Configuration,
        provider: CredentialsProvider?,
    ): Boolean = delegate.accept(connectAddress, remoteAddress, serverKey, config.withoutAsking(), provider)
}

private fun ServerKeyDatabase.Configuration.withoutAsking(): ServerKeyDatabase.Configuration {
    val checking = strictHostKeyChecking
    if (checking != StrictHostKeyChecking.ASK) return this
    return object : ServerKeyDatabase.Configuration by this {
        override fun getStrictHostKeyChecking() = StrictHostKeyChecking.ACCEPT_NEW
    }
}


// if known_hosts file in the .ssh directory is not present create it with correct permissions on unix (doesnt work on windows)
private fun createKnownHostsFile(sshDir: File) {
    val knownHosts = File(sshDir, KNOWN_HOSTS_FILE)
    if (knownHosts.exists()) return
    runCatching {
        if (!sshDir.exists() && sshDir.mkdirs()) {
            // not on Windows, whose files have no posix permissions - its user folder is private already
            runCatching { Files.setPosixFilePermissions(sshDir.toPath(), PosixFilePermissions.fromString("rwx------")) }
        }
        knownHosts.createNewFile()
    }
}
