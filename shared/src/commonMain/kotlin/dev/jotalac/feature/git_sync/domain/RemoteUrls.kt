package dev.jotalac.feature.git_sync.domain

import dev.jotalac.core.utils.isDesktopPlatform

// the short form git accepts for ssh: user@host:path
private val ScpLikeSshUrl = Regex("""[^\s/@:]+@[^\s/:]+:\S+""")

// whether remote url is ssh url
fun isSshRemote(remoteUrl: String?): Boolean {
    val url = remoteUrl?.trim() ?: return false
    return url.startsWith("ssh://") || url.startsWith("git+ssh://") || ScpLikeSshUrl.matches(url)
}

// if ssh is supported on device (supported only for desktop devices)
val isSshSupported: Boolean get() = isDesktopPlatform

// if notebook have all required remote credentials (differs from http to ssh)
fun hasRemoteCredentials(remoteUrl: String, username: String?, tokenOrPassword: String?): Boolean =
    isSshRemote(remoteUrl) || !username.isNullOrBlank() && !tokenOrPassword.isNullOrBlank()


// if notebook can be synced (all required credentials are present)
fun canSync(remoteUrl: String?, tokenOrPassword: String?): Boolean = when {
    remoteUrl.isNullOrBlank() -> false
    isSshRemote(remoteUrl) -> isSshSupported
    else -> !tokenOrPassword.isNullOrBlank()
}
