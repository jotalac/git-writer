package dev.jotalac.feature.notebooks_management.ui

fun validateRemoteUrl(
    remoteUrl: String,
): String? {
    return if (!remoteUrl.startsWith("http://") && !remoteUrl.startsWith("https://")) {
        "Remote URL must be over HTTP"
    } else if (!remoteUrl.endsWith(".git")) {
        "Remote URL must end with .git"
    } else {
        null
    }
}