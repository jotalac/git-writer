package dev.jotalac.feature.git_sync.data

import dev.jotalac.feature.git_sync.domain.isSshRemote
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

// credentials for remote repository - for both http and ssh
internal class GitCredentials(username: String?, val secret: String) : CredentialsProvider() {

    // without a username the token is sent as the username too - git hosts accept a token in either field
    private val http = UsernamePasswordCredentialsProvider(if (username.isNullOrBlank()) secret else username, secret)

    override fun isInteractive(): Boolean = false

    override fun supports(vararg items: CredentialItem): Boolean = http.supports(*items)

    override fun get(uri: URIish?, vararg items: CredentialItem): Boolean =
        uri?.let { isSshRemote(it.toString()) } != true && http.get(uri, *items)
}
