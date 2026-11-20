package dev.jotalac.feature.app_update.domain

// the latest release with the version and download url
data class LatestRelease(val version: String, val url: String)

// the state of the last version check
data class AppUpdateState(
    val latestRelease: LatestRelease? = null,
    val lastCheckedAtMillis: Long? = null,
)

// place to look up for the releases - can be any platform
fun interface ReleaseSource {
    suspend fun fetchLatestRelease(): LatestRelease
}


fun isNewerVersion(candidate: String, current: String): Boolean {
    val new = versionNumbers(candidate)
    val old = versionNumbers(current)
    for (i in 0 until maxOf(new.size, old.size)) {
        val a = new.getOrElse(i) { 0 }
        val b = old.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}

// convert version string to a list of numbers
private fun versionNumbers(version: String): List<Int> =
    version.trim().removePrefix("v").substringBefore('-')
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
