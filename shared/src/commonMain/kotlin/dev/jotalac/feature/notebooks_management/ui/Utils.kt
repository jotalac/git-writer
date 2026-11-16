package dev.jotalac.feature.notebooks_management.ui

import dev.jotalac.feature.git_sync.domain.isSshRemote
import dev.jotalac.feature.git_sync.domain.isSshSupported

// validate the remote repo url string, null when remote is correct
fun validateRemoteUrl(
    remoteUrl: String,
): String? {
    val isSsh = isSshRemote(remoteUrl)
    return when {
        isSsh && !isSshSupported -> "SSH remotes work only in the desktop app - use the HTTPS URL with a token"
        !isSsh && !remoteUrl.startsWith("http://") && !remoteUrl.startsWith("https://") ->
            if (isSshSupported) "Remote URL must be over HTTP or SSH" else "Remote URL must be over HTTP"
        !remoteUrl.endsWith(".git") -> "Remote URL must end with .git"
        else -> null
    }
}
